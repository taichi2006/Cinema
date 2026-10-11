package com.cinema.payment;

import com.cinema.booking.entity.Booking;
import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.payment.dto.CreatePaymentRequest;
import com.cinema.payment.dto.CreatePaymentResponse;
import com.cinema.payment.dto.PaymentResponse;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.PaymentMethod;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentType;
import com.cinema.payment.gateway.VnPayGateway;
import com.cinema.payment.gateway.VnPayIpnResult;
import com.cinema.payment.gateway.VnPayIpnResponse;
import com.cinema.voucher.Voucher;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaymentService {

    private static final Logger LOGGER = Logger.getLogger(PaymentService.class.getName());

    private final PaymentDAO paymentDAO;
    private final WalletDAO walletDAO;
    private final WalletTransactionDAO walletTransactionDAO;
    private final PaymentFulfillmentDAO fulfillmentDAO;
    private final VnPayGateway vnPayGateway;

    public PaymentService() {
        this(new PaymentDAO(), new WalletDAO(), new WalletTransactionDAO(),
                new PaymentFulfillmentDAO(), new VnPayGateway());
    }

    public PaymentService(PaymentDAO paymentDAO) {
        this(paymentDAO, new WalletDAO(), new WalletTransactionDAO(),
                new PaymentFulfillmentDAO(), new VnPayGateway());
    }

    PaymentService(PaymentDAO paymentDAO, WalletDAO walletDAO,
                   WalletTransactionDAO walletTransactionDAO, PaymentFulfillmentDAO fulfillmentDAO,
                   VnPayGateway vnPayGateway) {
        this.paymentDAO = paymentDAO;
        this.walletDAO = walletDAO;
        this.walletTransactionDAO = walletTransactionDAO;
        this.fulfillmentDAO = fulfillmentDAO;
        this.vnPayGateway = vnPayGateway;
    }

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
            if (paymentDAO.existsActivePendingByBookingId(bookingId, now, em)) {
                throw ApiException.conflict("Đơn hàng đang có giao dịch thanh toán chờ xử lý");
            }

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
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy ví của người dùng"));
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
            fulfillBooking(currentUserId, booking, payment, ticketSnapshots, now, em);
            em.getTransaction().commit();
            logPaymentEvent(Level.INFO, "wallet_payment_completed", payment, bookingId, null);

            return new CreatePaymentResponse(
                    payment.getId(), bookingId, payment.getAmount(), payment.getMethod(),
                    payment.getStatus(), null, null);
        } catch (ApiException exception) {
            rollbackIfNeeded(em);
            throw exception;
        } catch (Exception exception) {
            rollbackIfNeeded(em);
            throw ApiException.internal("Không thể xử lý thanh toán bằng ví");
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
            if (paymentDAO.existsActivePendingByBookingId(bookingId, now, em)) {
                throw ApiException.conflict("Đơn hàng đang có giao dịch thanh toán chờ xử lý");
            }
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

    static PaymentMethod requireSupportedMethod(CreatePaymentRequest request) {
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
        VnPayIpnResult ipn;
        try {
            ipn = vnPayGateway.verifyIpn(parameters);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "event=vnpay_ipn_verification_failed");
            return VnPayIpnResponse.of("99", "Unknown error");
        }
        if (!ipn.validSignature()) {
            return VnPayIpnResponse.of("97", "Invalid Checksum");
        }
        if (ipn.paymentId() == null || ipn.amount() == null) {
            return VnPayIpnResponse.of("99", "Invalid request data");
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Payment existingPayment = em.find(Payment.class, ipn.paymentId());
            if (existingPayment == null) {
                em.getTransaction().commit();
                return VnPayIpnResponse.of("01", "Order not found");
            }

            Booking booking = em.find(Booking.class, existingPayment.getBookingId(), LockModeType.PESSIMISTIC_WRITE);
            Payment payment = paymentDAO.findByIdForUpdate(ipn.paymentId(), em)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch thanh toán"));

            if (payment.getMethod() != PaymentMethod.VNPAY
                    || payment.getPaymentType() != PaymentType.BOOKING_PAYMENT) {
                em.getTransaction().commit();
                return VnPayIpnResponse.of("01", "Order not found");
            }
            if (payment.getAmount().compareTo(ipn.amount()) != 0) {
                em.getTransaction().commit();
                return VnPayIpnResponse.of("04", "Invalid amount");
            }
            if (payment.getStatus() == PaymentStatus.SUCCESSFUL) {
                em.getTransaction().commit();
                logPaymentEvent(Level.INFO, "vnpay_ipn_duplicate", payment, payment.getBookingId(),
                        ipn.transactionNo());
                return VnPayIpnResponse.of("02", "Order already confirmed");
            }
            if (payment.getStatus() == PaymentStatus.FAILED) {
                em.getTransaction().commit();
                return VnPayIpnResponse.of("00", "Confirm Success");
            }

            LocalDateTime now = LocalDateTime.now();
            if (ipn.paymentSuccessful()) {
                validateBookingForVnPayIpn(booking, now);
                List<Long> lockedSeatIds = fulfillmentDAO.lockHeldSeatIds(booking.getBookingId(), em);
                List<PaymentFulfillmentDAO.TicketSnapshot> ticketSnapshots =
                        fulfillmentDAO.findTicketSnapshots(booking.getBookingId(), em);
                if (lockedSeatIds.isEmpty() || ticketSnapshots.size() != lockedSeatIds.size()) {
                    throw ApiException.conflict("Dữ liệu ghế của đơn hàng không còn hợp lệ");
                }
                payment.setGatewayTransactionId(ipn.transactionNo());
                fulfillBooking(booking.getUserId(), booking, payment, ticketSnapshots, now, em);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("VNPAY response=" + ipn.responseCode()
                        + ", transactionStatus=" + ipn.transactionStatus());
            }

            em.getTransaction().commit();
            logPaymentEvent(Level.INFO, ipn.paymentSuccessful() ? "vnpay_ipn_fulfilled" : "vnpay_ipn_failed",
                    payment, booking.getBookingId(), ipn.transactionNo());
            return VnPayIpnResponse.of("00", "Confirm Success");
        } catch (Exception exception) {
            rollbackIfNeeded(em);
            LOGGER.log(Level.WARNING, "event=vnpay_ipn_processing_failed paymentId={0}", ipn.paymentId());
            return VnPayIpnResponse.of("99", "Unknown error");
        } finally {
            em.close();
        }
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

    private void validateBookingForVnPayIpn(Booking booking, LocalDateTime now) {
        if (booking == null || !"PENDING".equalsIgnoreCase(booking.getStatus())) {
            throw ApiException.conflict("Đơn hàng không ở trạng thái chờ thanh toán");
        }
        if (booking.getExpiredAt() != null && !booking.getExpiredAt().isAfter(now)) {
            throw ApiException.gone("Đã hết thời gian giữ ghế");
        }
    }

    private void fulfillBooking(long userId, Booking booking, Payment payment,
                                List<PaymentFulfillmentDAO.TicketSnapshot> ticketSnapshots,
                                LocalDateTime now, EntityManager em) {
        consumeVoucherIfPresent(userId, booking, now, em);

        int updatedSeats = fulfillmentDAO.markHeldSeatsBooked(booking.getBookingId(), em);
        if (updatedSeats != ticketSnapshots.size()) {
            throw ApiException.conflict("Không thể xác nhận đầy đủ ghế của đơn hàng");
        }

        fulfillmentDAO.createTickets(booking.getBookingId(), ticketSnapshots, em);
        payment.setStatus(PaymentStatus.SUCCESSFUL);
        payment.setPaymentDate(now);
        booking.setStatus("CONFIRMED");
        booking.setUpdatedAt(now);
    }

    private void consumeVoucherIfPresent(long userId, Booking booking, LocalDateTime now, EntityManager em) {
        if (booking.getVoucherId() == null) {
            return;
        }

        Voucher voucher = em.find(Voucher.class, booking.getVoucherId(), LockModeType.PESSIMISTIC_WRITE);
        if (voucher == null || !"ACTIVE".equalsIgnoreCase(voucher.getStatus())) {
            throw ApiException.conflict("Voucher không còn hiệu lực");
        }

        LocalDate today = now.toLocalDate();
        if ((voucher.getStartDate() != null && today.isBefore(voucher.getStartDate()))
                || (voucher.getEndDate() != null && today.isAfter(voucher.getEndDate()))) {
            throw ApiException.conflict("Voucher không còn trong thời gian áp dụng");
        }

        int usedCount = voucher.getUsedCount() == null ? 0 : voucher.getUsedCount();
        int quantity = voucher.getQuantity() == null ? 0 : voucher.getQuantity();
        if (quantity > 0 && usedCount >= quantity) {
            throw ApiException.conflict("Voucher đã hết lượt sử dụng");
        }

        voucher.setUsedCount(usedCount + 1);
        fulfillmentDAO.markUserVoucherUsed(userId, voucher.getVoucherId().longValue(), now, em);
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
