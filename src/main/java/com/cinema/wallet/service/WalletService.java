package com.cinema.wallet.service;

import com.cinema.common.dto.PageMeta;
import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.user.entity.User;
import com.cinema.wallet.dto.request.TopUpRequest;
import com.cinema.wallet.dto.response.*;
import com.cinema.wallet.entity.Wallet;
import com.cinema.wallet.entity.WalletTransaction;
import com.cinema.wallet.dao.WalletDAO;
import com.cinema.wallet.dao.WalletTransactionDAO;
import com.cinema.wallet.enums.WalletStatus;
import com.cinema.wallet.enums.TransactionType;
import com.cinema.wallet.enums.TransactionStatus;
import com.cinema.common.dto.PageResponse;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class WalletService {

    private final WalletDAO walletDAO = new WalletDAO();
    private final WalletTransactionDAO txDAO = new WalletTransactionDAO();

    private <T> T executeInTransaction(java.util.function.Function<EntityManager, T> action, String errorMsgPrefix) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            T result = action.apply(em);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            if (e instanceof ApiException) {
                throw (ApiException) e;
            }
            throw ApiException.internal(errorMsgPrefix + ": " + e.getMessage());
        } finally {
            em.close();
        }
    }

    public Wallet getOrCreateWallet(long userId) {
        return walletDAO.findByUserId(userId).orElseGet(() ->
            executeInTransaction(em -> {
                User user = em.find(User.class, userId);
                if (user == null) {
                    throw ApiException.notFound("Không tìm thấy người dùng với ID: " + userId);
                }
                Wallet wallet = new Wallet(user);
                wallet.setBalance(BigDecimal.ZERO);
                wallet.setStatus(WalletStatus.ACTIVE);
                em.persist(wallet);
                return wallet;
            }, "Lỗi khởi tạo ví người dùng")
        );
    }

    public WalletResponse getMyWallet(long userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return new WalletResponse(
                wallet.getId(),
                wallet.getUser().getId(),
                wallet.getBalance(),
                wallet.getStatus()
        );
    }

    public TopUpResponse topUp(long userId, TopUpRequest req) {
        if (req == null || req.getAmount() == null) {
            throw ApiException.badRequest("Số tiền nạp không được để trống");
        }
        if (req.getAmount() < 10000L || req.getAmount() > 50000000L) {
            throw ApiException.badRequest("Số tiền nạp tối thiểu là 10,000 VND và tối đa là 50,000,000 VND");
        }

        return executeInTransaction(em -> {
            Wallet wallet = em.createQuery("SELECT w FROM Wallet w JOIN FETCH w.user WHERE w.user.id = :userId", Wallet.class)
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
            }

            if (wallet.getStatus() == WalletStatus.SUSPENDED) {
                throw ApiException.forbidden("Ví của bạn đang bị tạm khóa (SUSPENDED)");
            }

            Instant now = Instant.now();
            BigDecimal amountBd = BigDecimal.valueOf(req.getAmount());

            wallet.setBalance(wallet.getBalance().add(amountBd));
            em.merge(wallet);

            WalletTransaction tx = new WalletTransaction();
            tx.setWallet(wallet);
            tx.setType(TransactionType.ADD_MONEY);
            tx.setStatus(TransactionStatus.SUCCESSFUL);
            tx.setAmount(amountBd);
            tx.setDescription("Nạp tiền vào ví thành công");
            em.persist(tx);

            // TopUpResponse constructor: paymentId, amount, method, status, walletTransactionId, paymentDate
            return new TopUpResponse(
                    null, // paymentId (GATEWAY) - không dùng ở đây
                    tx.getAmount(),
                    (req.getMethod() != null && !req.getMethod().isBlank()) ? req.getMethod().trim() : "BANK_TRANSFER",
                    tx.getStatus().name(),
                    tx.getId(),
                    now.toString()
            );
        }, "Lỗi nạp tiền vào ví");
    }

    public TopUpResponse getTopUpStatus(long userId, long topupId) {
        WalletTransaction tx = txDAO.findById(topupId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch nạp tiền #" + topupId));

        if (tx.getWallet().getUser().getId() != userId || tx.getType() != TransactionType.ADD_MONEY) {
            throw ApiException.notFound("Không tìm thấy giao dịch nạp tiền #" + topupId);
        }

        Instant created = tx.getCreatedAt() != null ? tx.getCreatedAt() : Instant.now();

        return new TopUpResponse(
                null,
                tx.getAmount(),
                "GATEWAY",
                tx.getStatus().name(),
                tx.getId(),
                created.toString()
        );
    }

    private Instant parseDate(String dateStr, String errorMsg, boolean endOfDay) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            LocalDate date = LocalDate.parse(dateStr.trim());
            if (endOfDay) date = date.plusDays(1);
            return date.atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest(errorMsg);
        }
    }

    public PageResponse<TransactionItemResponse> getTransactionHistory(long userId, String typeStr, String fromStr, String toStr, int page, int size) {
        Wallet wallet = getOrCreateWallet(userId);

        if (typeStr != null && !typeStr.isBlank()) {
            String t = typeStr.trim().toUpperCase();
            if (!"DEPOSIT".equals(t) && !"WITHDRAWAL".equals(t) && !"PAYMENT".equals(t) && !"REFUND".equals(t)) {
                throw ApiException.badRequest("Loại giao dịch không hợp lệ (DEPOSIT, WITHDRAWAL, PAYMENT, REFUND)");
            }
        }

        Instant from = parseDate(fromStr, "Định dạng ngày bắt đầu không hợp lệ (YYYY-MM-DD)", false);
        Instant to = parseDate(toStr, "Định dạng ngày kết thúc không hợp lệ (YYYY-MM-DD)", true);

        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }

        int p = Math.max(page, 0);
        int s = (size <= 0) ? 20 : Math.min(size, 100);

        List<WalletTransaction> list = txDAO.findHistory(wallet.getId(), typeStr, from, to, p, s);
        long total = txDAO.countHistory(wallet.getId(), typeStr, from, to);

        List<TransactionItemResponse> items = new ArrayList<>();
        for (WalletTransaction t : list) {
            String createdStr = t.getCreatedAt() != null ? t.getCreatedAt().toString() : Instant.now().toString();

            items.add(new TransactionItemResponse(
                    t.getId(),
                    t.getWallet().getId(),
                    t.getAmount(),
                    t.getType() != null ? t.getType() : TransactionType.ADD_MONEY,
                    t.getStatus() != null ? t.getStatus() : TransactionStatus.SUCCESSFUL,
                    t.getDescription() != null ? t.getDescription() : "",
                    createdStr
            ));
        }

        PageMeta meta = new PageMeta(p, s, total);
        return new PageResponse<>(items, meta);
    }
}
