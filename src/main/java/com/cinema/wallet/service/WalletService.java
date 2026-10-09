package com.cinema.wallet.service;

import com.cinema.common.dto.CommonDTO.PageMeta;
import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.user.entity.User;
import com.cinema.wallet.dao.WalletDAO;
import com.cinema.wallet.dao.WalletTopupDAO;
import com.cinema.wallet.dao.WalletTransactionDAO;
import com.cinema.wallet.dto.request.TopUpRequest;
import com.cinema.wallet.dto.response.*;
import com.cinema.wallet.entity.Wallet;
import com.cinema.wallet.entity.WalletTopup;
import com.cinema.wallet.entity.WalletTransaction;
import com.cinema.wallet.enums.TransactionType;
import com.cinema.wallet.enums.WalletStatus;
import com.cinema.wallet.enums.WalletTopupStatus;
import com.cinema.wallet.exception.WalletException;
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
 * - Nạp tiền vào ví tự động thành công 
 * - Theo dõi kết quả nạp tiền 
 * - Xem lịch sử bút toán ví đã hoàn tất 
 */
public class WalletService {

    private final WalletDAO walletDAO = new WalletDAO();
    private final WalletTransactionDAO txDAO = new WalletTransactionDAO();

    /**
     * Lấy ví của user, nếu chưa có (user cũ tạo trước khi có module ví) thì tự động tạo mới ví rỗng.
     */
    public Wallet getOrCreateWallet(long userId) {
        return walletDAO.findByUserId(userId).orElseGet(() -> {
            EntityManager em = JPAUtil.getEntityManager();
            try {
                em.getTransaction().begin();
                User user = em.find(User.class, userId);
                if (user == null) {
                    throw ApiException.notFound("Không tìm thấy người dùng với ID: " + userId);
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
                throw ApiException.internal("Lỗi khởi tạo ví người dùng: " + e.getMessage());
            } finally {
                em.close();
            }
        });
    }

    /**
     * 1. GET /wallet: Lấy thông tin ví và số dư của người dùng hiện tại
     */
    public WalletResponse getMyWallet(long userId) {
        Wallet wallet = getOrCreateWallet(userId);

        return new WalletResponse(
                String.valueOf(wallet.getId()),
                wallet.getBalance()
        );
    }

    /**
     * 2. POST /wallet/top-up: Nạp tiền vào ví (tự động nạp thành công ngay lập tức và ghi nhận giao dịch)
     */
    public TopUpResponse topUp(long userId, TopUpRequest req) {

        // Validate số tiền nạp theo Swagger: tối thiểu 10,000 VND, tối đa 50,000,000 VND
        if (req == null || req.getAmount() == null) {
            throw WalletException.invalidAmount("Số tiền nạp không được để trống");
        }
        if (req.getAmount() < 10000L || req.getAmount() > 50000000L) {
            throw WalletException.invalidAmount("Số tiền nạp tối thiểu là 10,000 VND và tối đa là 50,000,000 VND");
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
                    throw WalletException.resourceNotFound("Không tìm thấy người dùng #" + userId);
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
            tx.setDescription("Nạp tiền vào ví thành công");
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
            throw ApiException.internal("Lỗi nạp tiền vào ví: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * 3. GET /wallet/top-up/{id}: Theo dõi kết quả nạp tiền từ bảng cinema.wallet_topups
     */
    public TopUpResponse getTopUpStatus(long userId, long topupId) {
        WalletTransaction tx = txDAO.findById(topupId)
                .orElseThrow(() -> WalletException.resourceNotFound("Không tìm thấy giao dịch nạp tiền #" + topupId));

        if (tx.getWallet().getUser().getId() != userId || tx.getType() != TransactionType.ADD_MONEY) {
            throw WalletException.resourceNotFound("Không tìm thấy giao dịch nạp tiền #" + topupId);
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
     * 4. GET /wallet/transaction: Lịch sử bút toán ví (từ bảng cinema.wallet_transactions)
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
                throw WalletException.invalidFilter("Định dạng ngày bắt đầu không hợp lệ (YYYY-MM-DD)", "from");
            }
        }

        Instant to = null;
        if (toStr != null && !toStr.isBlank()) {
            try {
                to = LocalDate.parse(toStr.trim()).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException e) {
                throw WalletException.invalidFilter("Định dạng ngày kết thúc không hợp lệ (YYYY-MM-DD)", "to");
            }
        }

        if (from != null && to != null && from.isAfter(to)) {
            throw WalletException.invalidFilter("Ngày bắt đầu không được lớn hơn ngày kết thúc", "from");
        }

        int p = Math.max(page, 0);
        int s = (size <= 0) ? 20 : Math.min(size, 100);

        List<WalletTransaction> list = txDAO.findHistory(wallet.getId(), typeStr, from, to, p, s);
        long total = txDAO.countHistory(wallet.getId(), typeStr, from, to);
        int totalPages = (int) Math.ceil((double) total / s);

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

        PageMeta meta = new PageMeta(p, s, total, totalPages);
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
