package com.cinema.wallet.service;

import com.cinema.common.dto.PageMeta;
import com.cinema.common.dto.PageResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.PaymentMethod;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentType;
import com.cinema.payment.gateway.VnPayGateway;
import com.cinema.user.entity.User;
import com.cinema.wallet.dao.WalletDAO;
import com.cinema.wallet.dao.WalletTransactionDAO;
import com.cinema.wallet.dto.request.TopUpRequest;
import com.cinema.wallet.dto.response.TopUpResponse;
import com.cinema.wallet.dto.response.TransactionItemResponse;
import com.cinema.wallet.dto.response.WalletResponse;
import com.cinema.wallet.entity.Wallet;
import com.cinema.wallet.entity.WalletTransaction;
import com.cinema.wallet.enums.TransactionStatus;
import com.cinema.wallet.enums.TransactionType;
import com.cinema.wallet.enums.WalletStatus;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class WalletService {

    private final WalletDAO walletDAO = new WalletDAO();
    private final WalletTransactionDAO txDAO = new WalletTransactionDAO();

    // 1. Lấy hoặc tự động tạo ví
    public Wallet getOrCreateWallet(long userId) {
        return walletDAO.getOrCreate(userId);
    }

    // 2. Xem số dư ví
    public WalletResponse getMyWallet(long userId) {
        Wallet wallet = walletDAO.getOrCreate(userId);
        return new WalletResponse(wallet.getId(), wallet.getUser().getId(), wallet.getBalance(), wallet.getStatus());
    }

    // 3. Nạp tiền vào ví qua VNPAY
    public TopUpResponse topUp(long userId, TopUpRequest req) {
        return topUp(userId, req, null);
    }

    public TopUpResponse topUp(long userId, TopUpRequest req, String clientIp) {
        if (req == null || req.getAmount() == null) {
            throw ApiException.badRequest("Số tiền nạp không được để trống");
        }
        if (req.getAmount() < 10000L || req.getAmount() > 50000000L) {
            throw ApiException.badRequest("Số tiền nạp phải từ 10,000 đến 50,000,000 VND");
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Wallet wallet = em.createQuery("SELECT w FROM Wallet w WHERE w.user.id = :userId", Wallet.class)
                    .setParameter("userId", userId)
                    .getResultStream().findFirst().orElse(null);

            if (wallet == null) {
                User user = em.find(User.class, userId);
                if (user == null) {
                    throw ApiException.notFound("Không tìm thấy người dùng #" + userId);
                }
                wallet = new Wallet(user);
                wallet.setBalance(BigDecimal.ZERO);
                wallet.setStatus(WalletStatus.ACTIVE);
                em.persist(wallet);
                em.flush();
            }

            if (wallet.getStatus() == WalletStatus.SUSPENDED) {
                throw ApiException.forbidden("Ví của bạn đang bị tạm khóa (SUSPENDED)");
            }

            BigDecimal amountBd = BigDecimal.valueOf(req.getAmount());
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiredAt = now.plusMinutes(15);

            // 1. Tạo giao dịch ví ở trạng thái PENDING
            WalletTransaction tx = new WalletTransaction();
            tx.setWallet(wallet);
            tx.setType(TransactionType.ADD_MONEY);
            tx.setStatus(TransactionStatus.PENDING);
            tx.setAmount(amountBd);
            tx.setDescription("Nạp tiền vào ví qua VNPAY");
            em.persist(tx);
            em.flush();

            // 2. Tạo bản ghi Payment liên kết
            Payment payment = new Payment();
            payment.setAmount(amountBd);
            payment.setMethod(PaymentMethod.VNPAY);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentType(PaymentType.TOPUP);
            payment.setWalletTransactionId(tx.getId());
            payment.setExpiredAt(expiredAt);
            em.persist(payment);
            em.flush();

            // 3. Tạo link thanh toán VNPAY Sandbox thật
            VnPayGateway gateway = new VnPayGateway();
            String paymentUrl = gateway.createPaymentUrl(payment, clientIp, now);

            em.getTransaction().commit();

            TopUpResponse resp = new TopUpResponse(
                    payment.getId(), amountBd, "VNPAY", "PENDING",
                    tx.getId(), now.toString()
            );
            resp.setPaymentUrl(paymentUrl);
            resp.setExpiredAt(expiredAt.toString());
            return resp;
        } catch (ApiException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ApiException.internal("Lỗi tạo giao dịch nạp tiền VNPAY: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    // 4. Kiểm tra trạng thái giao dịch nạp tiền
    public TopUpResponse getTopUpStatus(long userId, long topupId) {
        WalletTransaction tx = txDAO.findById(topupId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch nạp tiền #" + topupId));

        if (tx.getWallet().getUser().getId() != userId || tx.getType() != TransactionType.ADD_MONEY) {
            throw ApiException.notFound("Không tìm thấy giao dịch nạp tiền #" + topupId);
        }

        String created = tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : Instant.now().toString();
        return new TopUpResponse(null, tx.getAmount(), "VNPAY", tx.getStatus().name(), tx.getId(), created);
    }

    // 5. Lịch sử giao dịch ví
    public PageResponse<TransactionItemResponse> getTransactionHistory(
            long userId, String type, String fromStr, String toStr, int page, int size) {
        Wallet wallet = walletDAO.getOrCreate(userId);

        Instant from = parseDate(fromStr, false);
        Instant to = parseDate(toStr, true);
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }

        int p = Math.max(page, 0);
        int s = (size <= 0) ? 20 : Math.min(size, 100);

        List<TransactionItemResponse> items = txDAO.findHistory(wallet.getId(), type, from, to, p, s).stream()
                .map(t -> new TransactionItemResponse(
                        t.getId(), t.getWallet().getId(), t.getAmount(),
                        t.getType() != null ? t.getType() : TransactionType.ADD_MONEY,
                        t.getStatus() != null ? t.getStatus() : TransactionStatus.SUCCESSFUL,
                        t.getDescription() != null ? t.getDescription() : "",
                        t.getCreatedAt() != null ? t.getCreatedAt().toString() : Instant.now().toString()
                )).toList();
        long total = txDAO.countHistory(wallet.getId(), type, from, to);

        return PageResponse.of(items, new PageMeta(p, s, total));
    }

    private Instant parseDate(String dateStr, boolean endOfDay) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            LocalDate date = LocalDate.parse(dateStr.trim());
            if (endOfDay) date = date.plusDays(1);
            return date.atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            throw ApiException.badRequest("Định dạng ngày không hợp lệ (YYYY-MM-DD): " + dateStr);
        }
    }
}
