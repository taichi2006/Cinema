package com.cinema.wallet;

import com.cinema.common.dto.PageMeta;
import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.user.entity.User;
import com.cinema.wallet.dto.request.TopUpRequest;
import com.cinema.wallet.dto.response.*;
import jakarta.persistence.EntityManager;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý toàn bộ nghiệp vụ của module Wallet:
 * - Xem số dư ví 
 * - NÃ¡ÂºÂ¡p tiÃ¡Â»Ân vÃƒÂ o ví tự Ã„â€˜Ã¡Â»â„¢ng thành công 
 * - Theo dõi kết quả nạp tiÃ¡Â»Ân 
 * - Xem lÃ¡Â»â€¹ch sÃ¡Â»Â­ bÃƒÂºt toÃƒÂ¡n ví Ã„â€˜ÃƒÂ£ hoÃƒÂ n tÃ¡ÂºÂ¥t 
 */
public class WalletService {

    private final WalletDAO walletDAO = new WalletDAO();
    private final WalletTransactionDAO txDAO = new WalletTransactionDAO();

    /**
     * LÃ¡ÂºÂ¥y ví cÃ¡Â»Â§a user, nếu chÃ†Â°a cÃƒÂ³ (user cÃ…Â© tÃ¡ÂºÂ¡o trÃ†Â°Ã¡Â»â€ºc khi cÃƒÂ³ module ví) thÃƒÂ¬ tự Ã„â€˜Ã¡Â»â„¢ng tÃ¡ÂºÂ¡o mÃ¡Â»â€ºi ví rÃ¡Â»â€”ng.
     */
    public Wallet getOrCreateWallet(long userId) {
        return walletDAO.findByUserId(userId).orElseGet(() -> {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                em.getTransaction().begin();
                User user = em.find(User.class, userId);
                if (user == null) {
                    throw ApiException.notFound("Không tìm thấy ngÃ†Â°Ã¡Â»Âi dùng vÃ¡Â»â€ºi ID: " + userId);
                }
                Wallet wallet = new Wallet(user);
                wallet.setBalance(0L);
                wallet.setStatus(WalletStatus.ACTIVE);
                em.persist(wallet);
                em.getTransaction().commit();
                return wallet;
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw ApiException.internal("Lỗi khÃ¡Â»Å¸i tÃ¡ÂºÂ¡o ví ngÃ†Â°Ã¡Â»Âi dùng: " + e.getMessage());
            } finally {
                em.close();
            }
        });
    }

    /**
     * 1. GET /wallet: LÃ¡ÂºÂ¥y thÃƒÂ´ng tin ví vÃƒÂ  số dư cÃ¡Â»Â§a ngÃ†Â°Ã¡Â»Âi dùng hiÃ¡Â»â€¡n tÃ¡ÂºÂ¡i
     */
    public WalletResponse getMyWallet(long userId) {
        Wallet wallet = getOrCreateWallet(userId);

        return new WalletResponse(
                String.valueOf(wallet.getId()),
                wallet.getBalance()
        );
    }

    /**
     * 2. POST /wallet/top-up: NÃ¡ÂºÂ¡p tiÃ¡Â»Ân vÃƒÂ o ví (tự Ã„â€˜Ã¡Â»â„¢ng nạp thành công ngay lÃ¡ÂºÂ­p tÃ¡Â»Â©c vÃƒÂ  ghi nhận giao dịch)
     */
    public TopUpResponse topUp(long userId, TopUpRequest req) {

        // Validate số tiÃ¡Â»Ân nạp theo Swagger: tối thiểu 10,000 VND, tối đa 50,000,000 VND
        if (req == null || req.getAmount() == null) {
            throw WalletException.invalidAmount("Số tiÃ¡Â»Ân nạp khÃƒÂ´ng được để trống");
        }
        if (req.getAmount() < 10000L || req.getAmount() > 50000000L) {
            throw WalletException.invalidAmount("Số tiÃ¡Â»Ân nạp tối thiểu lÃƒÂ  10,000 VND vÃƒÂ  tối đa lÃƒÂ  50,000,000 VND");
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Wallet wallet = em.createQuery("SELECT w FROM Wallet w JOIN FETCH w.user WHERE w.user.id = :userId", Wallet.class)
                    .setParameter("userId", userId)
                    .getResultStream().findFirst().orElse(null);

            if (wallet == null) {
                User user = em.find(User.class, userId);
                if (user == null) {
                    throw WalletException.resourceNotFound("Không tìm thấy ngÃ†Â°Ã¡Â»Âi dùng #" + userId);
                }
                wallet = new Wallet(user);
                wallet.setBalance(0L);
                wallet.setStatus(WalletStatus.ACTIVE);
                em.persist(wallet);
            }

            // Chặn giao dịch nếu ví đang bị tạm khóa
            if (wallet.getStatus() == WalletStatus.SUSPENDED) {
                throw WalletException.walletSuspended();
            }

            Instant now = Instant.now();
            Instant expiresAt = now.plus(Duration.ofMinutes(15));

            // Cập nhật số dư ví
            long newBalance = wallet.getBalance() + req.getAmount();
            wallet.setBalance(newBalance);
            em.merge(wallet);

            // Ghi nhận giao dịch
            WalletTransaction tx = new WalletTransaction();
            tx.setWallet(wallet);
            tx.setType(TransactionType.ADD_MONEY);
            tx.setStatus("SUCCESSFUL");
            tx.setAmount(req.getAmount());
            tx.setDescription("NÃ¡ÂºÂ¡p tiÃ¡Â»Ân vÃƒÂ o ví thành công");
            em.persist(tx);

            em.getTransaction().commit();

            return new TopUpResponse(
                    String.valueOf(tx.getId()),
                    tx.getAmount(),
                    (req.getMethod() != null && !req.getMethod().isBlank()) ? req.getMethod().trim() : "BANK_TRANSFER",
                    tx.getStatus(),
                    "/wallet/top-up/" + tx.getId(),
                    expiresAt.toString(),
                    now.toString(),
                    now.toString(),
                    null
            );
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            if (e instanceof ApiException) throw (ApiException) e;
            throw ApiException.internal("Lỗi nạp tiÃ¡Â»Ân vÃƒÂ o ví: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * 3. GET /wallet/top-up/{id}: Theo dõi kết quả nạp tiÃ¡Â»Ân từ bảng cinema.wallet_topups
     */
    public TopUpResponse getTopUpStatus(long userId, long topupId) {
        WalletTransaction tx = txDAO.findById(topupId)
                .orElseThrow(() -> WalletException.resourceNotFound("Không tìm thấy giao dịch nạp tiÃ¡Â»Ân #" + topupId));

        if (tx.getWallet().getUser().getId() != userId || tx.getType() != TransactionType.ADD_MONEY) {
            throw WalletException.resourceNotFound("Không tìm thấy giao dịch nạp tiÃ¡Â»Ân #" + topupId);
        }

        Instant created = tx.getCreatedAt() != null ? tx.getCreatedAt() : Instant.now();
        Instant expiresAt = created.plus(Duration.ofMinutes(15));
        String completedAt = "COMPLETED".equals(tx.getStatus()) ? created.toString() : null;

        return new TopUpResponse(
                String.valueOf(tx.getId()),
                tx.getAmount(),
                "GATEWAY",
                tx.getStatus(),
                "PENDING".equals(tx.getStatus()) ? "/wallet/top-up/" + tx.getId() : null,
                expiresAt.toString(),
                created.toString(),
                completedAt,
                null
        );
    }

    /**
     * 4. GET /wallet/transaction: LÃ¡Â»â€¹ch sÃ¡Â»Â­ bÃƒÂºt toÃƒÂ¡n ví (từ bảng cinema.wallet_transactions)
     */
    public HistoryResult getTransactionHistory(long userId, String typeStr, String fromStr, String toStr, int page, int size) {
        Wallet wallet = getOrCreateWallet(userId);

        if (typeStr != null && !typeStr.isBlank()) {
            String t = typeStr.trim().toUpperCase();
            if (!"DEPOSIT".equals(t) && !"WITHDRAWAL".equals(t) && !"PAYMENT".equals(t) && !"REFUND".equals(t)) {
                throw WalletException.invalidFilter("Loại giao dịch không hợp lệ (DEPOSIT, WITHDRAWAL, PAYMENT, REFUND)", "type");
            }
        }

        Instant from = null;
        if (fromStr != null && !fromStr.isBlank()) {
            try {
                from = LocalDate.parse(fromStr.trim()).atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException e) {
                throw WalletException.invalidFilter("Ã„ÂÃ¡Â»â€¹nh dạng ngày bắt Ã„â€˜Ã¡ÂºÂ§u khÃƒÂ´ng hợp lệ (YYYY-MM-DD)", "from");
            }
        }

        Instant to = null;
        if (toStr != null && !toStr.isBlank()) {
            try {
                to = LocalDate.parse(toStr.trim()).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException e) {
                throw WalletException.invalidFilter("Ã„ÂÃ¡Â»â€¹nh dạng ngày kết thÃƒÂºc khÃƒÂ´ng hợp lệ (YYYY-MM-DD)", "to");
            }
        }

        if (from != null && to != null && from.isAfter(to)) {
            throw WalletException.invalidFilter("Ngày bắt đầu không được lớn hơn ngày kết thúc", "from");
        }

        int p = Math.max(page, 0);
        int s = (size <= 0) ? 20 : Math.min(size, 100);

        List<WalletTransaction> list = txDAO.findHistory(wallet.getId(), typeStr, from, to, p, s);
        long total = txDAO.countHistory(wallet.getId(), typeStr, from, to);

        List<TransactionItemResponse> items = new ArrayList<>();
        for (WalletTransaction t : list) {
            String createdStr = t.getCreatedAt() != null ? t.getCreatedAt().toString() : Instant.now().toString();

            items.add(new TransactionItemResponse(
                    String.valueOf(t.getId()),
                    t.getType() != null ? t.getType().name() : "DEPOSIT",
                    t.getStatus(),
                    t.getAmount(),
                    t.getDescription() != null ? t.getDescription() : "",
                    createdStr
            ));
        }

        PageMeta meta = new PageMeta(p, s, total);
        return new HistoryResult(items, meta);
    }

    public static class HistoryResult {
        private List<TransactionItemResponse> items;
        private PageMeta meta;

        public HistoryResult() {}

        public HistoryResult(List<TransactionItemResponse> items, PageMeta meta) {
            this.items = items;
            this.meta = meta;
        }

        public List<TransactionItemResponse> getItems() {
            return items;
        }

        public void setItems(List<TransactionItemResponse> items) {
            this.items = items;
        }

        public PageMeta getMeta() {
            return meta;
        }

        public void setMeta(PageMeta meta) {
            this.meta = meta;
        }
    }
}
