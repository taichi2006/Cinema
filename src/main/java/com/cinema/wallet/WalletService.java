package com.cinema.wallet;

import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.user.User;
import com.cinema.common.dto.CommonDTO.PageMeta;
import com.cinema.wallet.dto.request.AdminConfirmRequest;
import com.cinema.wallet.dto.request.TopUpRequest;
import com.cinema.wallet.dto.response.*;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
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
 * - Tạo yêu cầu nạp tiền (PENDING)
 * - Theo dõi kết quả nạp tiền
 * - Xem lịch sử biến động số dư đã hoàn tất (SUCCESSFUL / SUCCEEDED)
 * - Quản trị viên (ADMIN) xem danh sách chờ và duyệt nạp tiền
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
                wallet.setBalance(BigDecimal.ZERO);
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
        Instant updated = wallet.getUpdatedAt() != null ? wallet.getUpdatedAt()
                : (wallet.getCreatedAt() != null ? wallet.getCreatedAt() : Instant.now());

        return new WalletResponse(
                String.valueOf(wallet.getId()),
                wallet.getBalance(),
                "VND",
                updated.toString()
        );
    }

    /**
     * 2. POST /wallet/top-up: Tạo yêu cầu nạp ví qua cổng (chưa cộng tiền)
     */
    public TopUpResponse topUp(long userId, TopUpRequest req, String idempotencyKey) {
        // Kiểm tra Idempotency-Key theo yêu cầu Swagger (16 - 128 ký tự)
        if (idempotencyKey == null || idempotencyKey.trim().length() < 16 || idempotencyKey.trim().length() > 128) {
            throw WalletException.idempotencyKeyRequired();
        }

        // Validate số tiền nạp theo Swagger: tối thiểu 10,000 VND, tối đa 10,000,000 VND
        if (req == null || req.getAmount() == null) {
            throw WalletException.invalidAmount("Số tiền nạp không được để trống");
        }
        if (req.getAmount().compareTo(new BigDecimal("10000")) < 0 || req.getAmount().compareTo(new BigDecimal("10000000")) > 0) {
            throw WalletException.invalidAmount("Số tiền nạp tối thiểu là 10,000 VND và tối đa là 10,000,000 VND");
        }

        Wallet wallet = getOrCreateWallet(userId);

        // Chặn giao dịch nếu ví đang bị tạm khóa (SUSPENDED)
        if (wallet.getStatus() == WalletStatus.SUSPENDED) {
            throw WalletException.walletSuspended();
        }

        // Tạo bản ghi giao dịch nạp tiền ở trạng thái PENDING
        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setAmount(req.getAmount());
        tx.setTransactionType(TransactionType.TOP_UP);
        tx.setStatus(TransactionStatus.PENDING);
        tx.setReferenceId(idempotencyKey.trim());
        tx.setDescription("Yêu cầu nạp tiền vào ví qua cổng thanh toán");

        tx = txDAO.save(tx);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(15)); // Hết hạn sau 15 phút

        return new TopUpResponse(
                String.valueOf(tx.getId()),
                tx.getAmount(),
                "VND",
                (req.getMethod() != null && !req.getMethod().isBlank()) ? req.getMethod() : "GATEWAY",
                tx.getStatus().name(),
                "/wallet/top-up/" + tx.getId(),
                expiresAt.toString(),
                now.toString(),
                null,
                null
        );
    }

    /**
     * 3. GET /wallet/top-up/{id}: Theo dõi kết quả nạp tiền
     */
    public TopUpResponse getTopUpStatus(long userId, long txId) {
        WalletTransaction tx = txDAO.findById(txId)
                .orElseThrow(() -> WalletException.resourceNotFound("Không tìm thấy giao dịch nạp tiền #" + txId));

        // Bảo mật: Đảm bảo giao dịch thuộc đúng ví của người dùng này
        if (tx.getWallet().getUser().getId() != userId) {
            throw WalletException.resourceNotFound("Không tìm thấy giao dịch nạp tiền #" + txId);
        }

        Instant created = tx.getCreatedAt() != null ? tx.getCreatedAt() : Instant.now();
        Instant expiresAt = created.plus(Duration.ofMinutes(15));
        String completedAt = tx.getStatus().isSuccessful() ? created.toString() : null;
        String failureCode = tx.getStatus() == TransactionStatus.FAILED ? "REJECTED_BY_ADMIN" : null;

        return new TopUpResponse(
                String.valueOf(tx.getId()),
                tx.getAmount(),
                "VND",
                "GATEWAY",
                tx.getStatus().name(),
                tx.getStatus() == TransactionStatus.PENDING ? "/wallet/top-up/" + tx.getId() : null,
                expiresAt.toString(),
                created.toString(),
                completedAt,
                failureCode
        );
    }

    /**
     * 4. GET /wallet/transaction: Lịch sử bút toán ví đã hoàn tất, sắp xếp mới nhất trước
     */
    public HistoryResult getTransactionHistory(long userId, String typeStr, String fromStr, String toStr, int page, int size) {
        Wallet wallet = getOrCreateWallet(userId);

        TransactionType type = null;
        if (typeStr != null && !typeStr.isBlank()) {
            try {
                type = TransactionType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw WalletException.invalidFilter("Loại giao dịch không hợp lệ (TOP_UP, PAYMENT, REFUND)", "type");
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

        List<WalletTransaction> list = txDAO.findHistory(wallet.getId(), type, from, to, p, s);
        long total = txDAO.countHistory(wallet.getId(), type, from, to);
        int totalPages = (int) Math.ceil((double) total / s);

        List<TransactionItemResponse> items = new ArrayList<>();
        for (WalletTransaction t : list) {
            String dir = (t.getTransactionType() == TransactionType.PAYMENT) ? "DEBIT" : "CREDIT";
            BigDecimal balAfter = t.getBalanceAfter() != null ? t.getBalanceAfter() : BigDecimal.ZERO;
            String refId = t.getReferenceId() != null ? t.getReferenceId() : String.valueOf(t.getId());
            String createdStr = t.getCreatedAt() != null ? t.getCreatedAt().toString() : Instant.now().toString();

            items.add(new TransactionItemResponse(
                    String.valueOf(t.getId()),
                    t.getTransactionType().name(),
                    dir,
                    t.getAmount(),
                    balAfter,
                    "VND",
                    t.getTransactionType().name(),
                    refId,
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

    // ==================== ADMIN OPERATIONS ====================

    /**
     * 5. GET /wallet/top-up/pending (ADMIN): Xem danh sách yêu cầu nạp tiền chờ duyệt
     */
    public PendingTopUpsResult getPendingTopUps(int page, int size) {
        int p = Math.max(page, 0);
        int s = (size <= 0) ? 20 : Math.min(size, 100);

        List<WalletTransaction> list = txDAO.findPendingTopUps(p, s);
        long total = txDAO.countPendingTopUps();
        int totalPages = (int) Math.ceil((double) total / s);

        List<AdminPendingItemResponse> items = new ArrayList<>();
        for (WalletTransaction t : list) {
            Instant created = t.getCreatedAt() != null ? t.getCreatedAt() : Instant.now();
            Instant expires = created.plus(Duration.ofMinutes(15));
            User u = t.getWallet().getUser();

            items.add(new AdminPendingItemResponse(
                    String.valueOf(t.getId()),
                    t.getWallet().getId(),
                    u.getId(),
                    u.getEmail(),
                    t.getAmount(),
                    "VND",
                    "GATEWAY",
                    t.getStatus().name(),
                    t.getDescription(),
                    created.toString(),
                    expires.toString()
            ));
        }

        PageMeta meta = new PageMeta(p, s, total, totalPages);
        return new PendingTopUpsResult(items, meta);
    }

    public static class PendingTopUpsResult {
        private List<AdminPendingItemResponse> items;
        private PageMeta meta;

        public PendingTopUpsResult() {}

        public PendingTopUpsResult(List<AdminPendingItemResponse> items, PageMeta meta) {
            this.items = items;
            this.meta = meta;
        }

        public List<AdminPendingItemResponse> getItems() {
            return items;
        }

        public void setItems(List<AdminPendingItemResponse> items) {
            this.items = items;
        }

        public PageMeta getMeta() {
            return meta;
        }

        public void setMeta(PageMeta meta) {
            this.meta = meta;
        }
    }

    /**
     * 6. POST /wallet/top-up/{id}/confirm (ADMIN): Duyệt hoặc từ chối nạp tiền
     */
    public AdminConfirmResponse confirmTopUp(long txId, AdminConfirmRequest req) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            WalletTransaction tx = em.find(WalletTransaction.class, txId);
            if (tx == null) {
                throw WalletException.resourceNotFound("Không tìm thấy giao dịch #" + txId);
            }

            if (tx.getStatus() != TransactionStatus.PENDING) {
                throw ApiException.badRequest("Giao dịch #" + txId + " không ở trạng thái PENDING (hiện tại: " + tx.getStatus() + ")");
            }

            boolean approve = req != null && Boolean.TRUE.equals(req.getApprove());
            Wallet wallet = tx.getWallet();
            Instant now = Instant.now();

            if (approve) {
                // Cộng tiền vào ví
                BigDecimal newBalance = wallet.getBalance().add(tx.getAmount());
                wallet.setBalance(newBalance);
                wallet.setUpdatedAt(now);
                em.merge(wallet);

                tx.setStatus(TransactionStatus.SUCCEEDED);
                tx.setBalanceAfter(newBalance);
                if (req.getNote() != null && !req.getNote().isBlank()) {
                    tx.setDescription(tx.getDescription() + " | Admin: " + req.getNote().trim());
                }
            } else {
                // Từ chối nạp tiền
                tx.setStatus(TransactionStatus.FAILED);
                if (req != null && req.getNote() != null && !req.getNote().isBlank()) {
                    tx.setDescription(tx.getDescription() + " | Từ chối: " + req.getNote().trim());
                }
            }

            em.merge(tx);
            em.getTransaction().commit();

            return new AdminConfirmResponse(
                    String.valueOf(tx.getId()),
                    wallet.getId(),
                    tx.getAmount(),
                    "VND",
                    tx.getStatus().name(),
                    wallet.getBalance(),
                    now.toString()
            );

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            if (e instanceof ApiException) throw (ApiException) e;
            throw ApiException.internal("Lỗi xử lý duyệt nạp tiền: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}
