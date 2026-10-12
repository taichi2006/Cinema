package com.cinema.payment;

import com.cinema.booking.entity.Booking;
import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.PaymentMethod;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentType;
import com.cinema.payment.gateway.VnPayGateway;
import com.cinema.payment.gateway.VnPayIpnResponse;
import com.cinema.payment.gateway.VnPayIpnResult;
import com.cinema.ticket.Email.TicketEmailService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaymentIpnService {

    private static final Logger LOGGER = Logger.getLogger(PaymentIpnService.class.getName());

    private final PaymentDAO paymentDAO;
    private final PaymentFulfillmentDAO fulfillmentDAO;
    private final VnPayGateway vnPayGateway;
    private final TicketEmailService ticketEmailService;

    public PaymentIpnService() {
        this(new PaymentDAO(), new PaymentFulfillmentDAO(), new VnPayGateway(), new TicketEmailService());
    }

    public PaymentIpnService(PaymentDAO paymentDAO, PaymentFulfillmentDAO fulfillmentDAO,
                             VnPayGateway vnPayGateway) {
        this(paymentDAO, fulfillmentDAO, vnPayGateway, new TicketEmailService());
    }

    public PaymentIpnService(PaymentDAO paymentDAO, PaymentFulfillmentDAO fulfillmentDAO,
                             VnPayGateway vnPayGateway, TicketEmailService ticketEmailService) {
        this.paymentDAO = paymentDAO;
        this.fulfillmentDAO = fulfillmentDAO;
        this.vnPayGateway = vnPayGateway;
        this.ticketEmailService = ticketEmailService != null ? ticketEmailService : new TicketEmailService();
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

            Booking booking = (existingPayment.getBookingId() != null)
                    ? em.find(Booking.class, existingPayment.getBookingId(), LockModeType.PESSIMISTIC_WRITE)
                    : null;
            Payment payment = paymentDAO.findByIdForUpdate(ipn.paymentId(), em)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch thanh toán"));

            if (payment.getMethod() != PaymentMethod.VNPAY) {
                em.getTransaction().commit();
                return VnPayIpnResponse.of("01", "Order not found");
            }
            if (payment.getAmount().compareTo(ipn.amount()) != 0) {
                em.getTransaction().commit();
                return VnPayIpnResponse.of("04", "Invalid amount");
            }
            if (payment.getStatus() == PaymentStatus.SUCCESSFUL) {
                em.getTransaction().commit();
                LOGGER.log(Level.INFO, "event=vnpay_ipn_duplicate paymentId={0}", payment.getId());
                return VnPayIpnResponse.of("02", "Order already confirmed");
            }
            if (payment.getStatus() == PaymentStatus.FAILED) {
                em.getTransaction().commit();
                return VnPayIpnResponse.of("00", "Confirm Success");
            }

            LocalDateTime now = LocalDateTime.now();

            if (payment.getPaymentType() == PaymentType.TOPUP) {
                if (ipn.paymentSuccessful()) {
                    payment.setStatus(PaymentStatus.SUCCESSFUL);
                    payment.setGatewayTransactionId(ipn.transactionNo());
                    payment.setPaymentDate(now);

                    if (payment.getWalletTransactionId() != null) {
                        com.cinema.wallet.entity.WalletTransaction tx =
                                em.find(com.cinema.wallet.entity.WalletTransaction.class, payment.getWalletTransactionId());
                        if (tx != null) {
                            tx.setStatus(com.cinema.wallet.enums.TransactionStatus.SUCCESSFUL);
                            tx.setDescription("Nạp tiền vào ví qua VNPAY thành công (GD: " + ipn.transactionNo() + ")");
                            com.cinema.wallet.entity.Wallet wallet = tx.getWallet();
                            wallet.setBalance(wallet.getBalance().add(payment.getAmount()));
                            em.merge(wallet);
                        }
                    }
                } else {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason("VNPAY response=" + ipn.responseCode());
                    payment.setPaymentDate(now);
                    if (payment.getWalletTransactionId() != null) {
                        com.cinema.wallet.entity.WalletTransaction tx =
                                em.find(com.cinema.wallet.entity.WalletTransaction.class, payment.getWalletTransactionId());
                        if (tx != null) {
                            tx.setStatus(com.cinema.wallet.enums.TransactionStatus.FAILED);
                            tx.setDescription("Nạp tiền vào ví qua VNPAY thất bại");
                        }
                    }
                }
                em.getTransaction().commit();
                return VnPayIpnResponse.of("00", "Confirm Success");
            }

            if (ipn.paymentSuccessful()) {
                validateBookingForVnPayIpn(booking, now);
                List<Long> lockedSeatIds = fulfillmentDAO.lockHeldSeatIds(booking.getBookingId(), em);
                List<PaymentFulfillmentDAO.TicketSnapshot> ticketSnapshots =
                        fulfillmentDAO.findTicketSnapshots(booking.getBookingId(), em);
                if (lockedSeatIds.isEmpty() || ticketSnapshots.size() != lockedSeatIds.size()) {
                    throw ApiException.conflict("Dữ liệu ghế của đơn hàng không còn hợp lệ");
                }
                payment.setGatewayTransactionId(ipn.transactionNo());
                fulfillmentDAO.fulfillBooking(booking.getUserId(), booking, payment, ticketSnapshots, now, em);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("VNPAY response=" + ipn.responseCode()
                        + ", transactionStatus=" + ipn.transactionStatus());
            }

            em.getTransaction().commit();
            LOGGER.log(Level.INFO, "event={0} paymentId={1} bookingId={2} gatewayTransactionId={3}",
                    new Object[]{ipn.paymentSuccessful() ? "vnpay_ipn_fulfilled" : "vnpay_ipn_failed",
                            payment.getId(), booking.getBookingId(), ipn.transactionNo()});

            if (ipn.paymentSuccessful()) {
                ticketEmailService.sendTicketEmailByBookingAsync(booking.getBookingId(), booking.getUserId());
            }

            return VnPayIpnResponse.of("00", "Confirm Success");
        } catch (Exception exception) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOGGER.log(Level.WARNING, "event=vnpay_ipn_processing_failed paymentId={0}", ipn.paymentId());
            return VnPayIpnResponse.of("99", "Unknown error");
        } finally {
            em.close();
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
}
