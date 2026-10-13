package com.cinema.payment.service;

import com.cinema.booking.entity.Booking;
import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.email.TicketEmailService;
import com.cinema.payment.dao.PaymentDAO;
import com.cinema.payment.dao.PaymentFulfillmentDAO;
import com.cinema.payment.dto.request.CreatePaymentRequest;
import com.cinema.payment.dto.response.CreatePaymentResponse;
import com.cinema.payment.dto.response.PaymentResponse;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.PaymentMethod;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentType;
import com.cinema.payment.gateway.VnPayGateway;
import com.cinema.payment.gateway.VnPayIpnResponse;
import com.cinema.user.entity.User;
import com.cinema.wallet.dao.WalletDAO;
import com.cinema.wallet.dao.WalletTransactionDAO;
import com.cinema.wallet.entity.Wallet;
import com.cinema.wallet.entity.WalletTransaction;
import com.cinema.wallet.enums.TransactionStatus;
import com.cinema.wallet.enums.TransactionType;
import com.cinema.wallet.enums.WalletStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaymentService {

    private static final Logger LOGGER = Logger.getLogger(PaymentService.class.getName());

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final WalletDAO walletDAO = new WalletDAO();
    private final WalletTransactionDAO walletTransactionDAO = new WalletTransactionDAO();
    private final PaymentFulfillmentDAO fulfillmentDAO = new PaymentFulfillmentDAO();
    private final VnPayGateway vnPayGateway = new VnPayGateway();
    private final PaymentIpnService ipnService = new PaymentIpnService();
    private final TicketEmailService ticketEmailService = new TicketEmailService();

    public List<PaymentResponse> getPaymentsByBooking(long currentUserId, long bookingId) {
        assertBookingOwnership(currentUserId, bookingId);
        return paymentDAO.findByBookingId(bookingId).stream()
                .map(PaymentResponse::new)
                .toList();
    }

    public PaymentResponse getPaymentDetail(long currentUserId, long paymentId) {
        Payment payment = paymentDAO.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch thanh toán"));
        assertBookingOwnership(currentUserId, payment.getBookingId());
        return new PaymentResponse(payment);
    }

    public CreatePaymentResponse createPayment(long currentUserId, long bookingId,
                                               CreatePaymentRequest request) {
        return createPayment(currentUserId, bookingId, request, null);
    }

    public CreatePaymentResponse createPayment(long currentUserId, long bookingId,
                                               CreatePaymentRequest request, String clientIp) {
        if (bookingId <= 0) {
            throw ApiException.badRequest("Mã đơn hàng không hợp lệ");
        }
        PaymentMethod method = requireSupportedMethod(request);
        return method == PaymentMethod.WALLET
                ? createWalletPayment(currentUserId, bookingId)
                : createVnPayPayment(currentUserId, bookingId, clientIp);
    }

    private CreatePaymentResponse createWalletPayment(long currentUserId, long bookingId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            LocalDateTime now = LocalDateTime.now();
            Booking booking = em.find(Booking.class, bookingId, LockModeType.PESSIMISTIC_WRITE);
            validateBookingForPayment(booking, currentUserId, now, em);

            if (paymentDAO.existsSuccessfulByBookingId(bookingId, em)) {
                throw ApiException.conflict("Đơn hàng đã được thanh toán");
            }
            // Hủy các giao dịch chờ cũ (nếu khách quay lại bấm thanh toán hoặc đổi sang Ví)
            paymentDAO.cancelPendingPaymentsByBookingId(bookingId, "Đổi sang thanh toán bằng ví", em);

            List<Long> lockedSeatIds = fulfillmentDAO.lockHeldSeatIds(bookingId, em);
            if (lockedSeatIds.isEmpty()) {
                throw ApiException.conflict("Đơn hàng không còn ghế đang được giữ");
            }
            List<PaymentFulfillmentDAO.TicketSnapshot> ticketSnapshots =
                    fulfillmentDAO.findTicketSnapshots(bookingId, em);
            if (ticketSnapshots.size() != lockedSeatIds.size()) {
                throw ApiException.conflict("Dữ liệu ghế của đơn hàng không còn hợp lệ");
            }

            Wallet wallet = walletDAO.findByUserIdForUpdate(currentUserId, em)
                    .orElseGet(() -> {
                        User user = em.find(User.class, currentUserId);
                        if (user == null) {
                            throw ApiException.notFound("Không tìm thấy người dùng");
                        }
                        Wallet newWallet = new Wallet(user);
                        newWallet.setBalance(BigDecimal.ZERO);
                        newWallet.setStatus(WalletStatus.ACTIVE);
                        em.persist(newWallet);
                        em.flush();
                        return newWallet;
                    });
            if (wallet.getStatus() != WalletStatus.ACTIVE) {
                throw ApiException.forbidden("Ví của bạn đang bị tạm khóa");
            }
            if (wallet.getBalance() == null || wallet.getBalance().compareTo(booking.getTotalAmount()) < 0) {
                throw ApiException.unprocessable("Số dư ví không đủ để thanh toán");
            }

            Payment payment = new Payment();
            payment.setBookingId(bookingId);
            payment.setAmount(booking.getTotalAmount());
            payment.setMethod(PaymentMethod.WALLET);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentType(PaymentType.BOOKING_PAYMENT);
            paymentDAO.save(payment, em);
            em.flush();

            wallet.setBalance(wallet.getBalance().subtract(payment.getAmount()));

            WalletTransaction walletTransaction = new WalletTransaction();
            walletTransaction.setWallet(wallet);
            walletTransaction.setAmount(payment.getAmount());
            walletTransaction.setType(TransactionType.PAYMENT);
            walletTransaction.setStatus(TransactionStatus.SUCCESSFUL);
            walletTransaction.setDescription("Thanh toán booking #" + bookingId);
            walletTransactionDAO.save(walletTransaction, em);
            em.flush();

            payment.setWalletTransactionId(walletTransaction.getId());
            fulfillmentDAO.fulfillBooking(currentUserId, booking, payment, ticketSnapshots, now, em);
            em.getTransaction().commit();
            logPaymentEvent(Level.INFO, "wallet_payment_completed", payment, bookingId, null);

            // Gửi email vé điện tử bất đồng bộ cho người dùng
            ticketEmailService.sendTicketEmailByBookingAsync(bookingId, currentUserId);

            return new CreatePaymentResponse(
                    payment.getId(), bookingId, payment.getAmount(), payment.getMethod(),
                    payment.getStatus(), null, null);
        } catch (ApiException exception) {
            rollbackIfNeeded(em);
            throw exception;
        } catch (Exception exception) {
            rollbackIfNeeded(em);
            LOGGER.log(Level.SEVERE, "Lỗi xử lý thanh toán ví cho booking #" + bookingId, exception);
            throw ApiException.internal("Không thể xử lý thanh toán bằng ví: " + (exception.getMessage() != null ? exception.getMessage() : "Lỗi hệ thống"));
        } finally {
            em.close();
        }
    }

    private CreatePaymentResponse createVnPayPayment(long currentUserId, long bookingId, String clientIp) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            LocalDateTime now = LocalDateTime.now();
            Booking booking = em.find(Booking.class, bookingId, LockModeType.PESSIMISTIC_WRITE);
            validateBookingForPayment(booking, currentUserId, now, em);

            if (paymentDAO.existsSuccessfulByBookingId(bookingId, em)) {
                throw ApiException.conflict("Đơn hàng đã được thanh toán");
            }
            // Hủy các giao dịch chờ cũ để tạo giao dịch VNPAY mới
            paymentDAO.cancelPendingPaymentsByBookingId(bookingId, "Tạo lại giao dịch VNPAY mới", em);
            if (fulfillmentDAO.lockHeldSeatIds(bookingId, em).isEmpty()) {
                throw ApiException.conflict("Đơn hàng không còn ghế đang được giữ");
            }
            if (booking.getExpiredAt() == null) {
                throw ApiException.unprocessable("Đơn hàng không có thời hạn thanh toán");
            }

            Payment payment = new Payment();
            payment.setBookingId(bookingId);
            payment.setAmount(booking.getTotalAmount());
            payment.setMethod(PaymentMethod.VNPAY);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentType(PaymentType.BOOKING_PAYMENT);
            payment.setExpiredAt(booking.getExpiredAt());
            paymentDAO.save(payment, em);
            em.flush();

            String paymentUrl = vnPayGateway.createPaymentUrl(payment, clientIp, now);
            em.getTransaction().commit();
            logPaymentEvent(Level.INFO, "vnpay_payment_created", payment, bookingId, null);
            return new CreatePaymentResponse(
                    payment.getId(), bookingId, payment.getAmount(), payment.getMethod(),
                    payment.getStatus(), paymentUrl, payment.getExpiredAt());
        } catch (ApiException exception) {
            rollbackIfNeeded(em);
            throw exception;
        } catch (Exception exception) {
            rollbackIfNeeded(em);
            throw ApiException.internal("Không thể tạo giao dịch VNPAY");
        } finally {
            em.close();
        }
    }

    public static PaymentMethod requireSupportedMethod(CreatePaymentRequest request) {
        if (request == null || request.getMethod() == null || request.getMethod().isBlank()) {
            throw ApiException.badRequest("Phương thức thanh toán không được để trống");
        }
        try {
            PaymentMethod method = PaymentMethod.valueOf(request.getMethod().trim().toUpperCase(Locale.ROOT));
            if (method != PaymentMethod.WALLET && method != PaymentMethod.VNPAY) {
                throw ApiException.unprocessable("Chỉ hỗ trợ phương thức WALLET hoặc VNPAY");
            }
            return method;
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("Phương thức thanh toán không hợp lệ");
        }
    }

    public VnPayIpnResponse handleVnPayIpn(Map<String, String[]> parameters) {
        return ipnService.handleVnPayIpn(parameters);
    }

    private void validateBookingForPayment(Booking booking, long currentUserId, LocalDateTime now,
                                           EntityManager em) {
        if (booking == null) {
            throw ApiException.notFound("Không tìm thấy đơn đặt vé");
        }
        if (!Long.valueOf(currentUserId).equals(booking.getUserId())) {
            throw ApiException.forbidden("Bạn không có quyền thanh toán đơn hàng này");
        }
        if (!"PENDING".equalsIgnoreCase(booking.getStatus())) {
            throw ApiException.conflict("Đơn hàng không ở trạng thái chờ thanh toán");
        }
        if (booking.getExpiredAt() != null && !booking.getExpiredAt().isAfter(now)) {
            throw ApiException.gone("Đã hết thời gian giữ ghế");
        }
        if (booking.getTotalAmount() == null || booking.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.unprocessable("Đơn hàng có số tiền không thể thanh toán");
        }
    }

    private void rollbackIfNeeded(EntityManager em) {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }

    private void logPaymentEvent(Level level, String event, Payment payment, long bookingId,
                                 String gatewayTransactionId) {
        LOGGER.log(level,
                "event={0} paymentId={1} bookingId={2} gatewayTransactionId={3}",
                new Object[]{event, payment.getId(), bookingId, gatewayTransactionId});
    }

    private void assertBookingOwnership(long currentUserId, long bookingId) {
        Long ownerId = paymentDAO.findBookingOwnerId(bookingId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn đặt vé"));
        if (!ownerId.equals(currentUserId)) {
            throw ApiException.forbidden("Bạn không có quyền xem giao dịch thanh toán của đơn này");
        }
    }
}
