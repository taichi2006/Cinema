package com.cinema.wallet.service;

import com.cinema.common.dto.CommonDTO.PageMeta;
import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
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
import com.cinema.wallet.exception.WalletException;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý toàn bộ nghiệp vụ của module Wallet:
 * - Xem số dư ví
 * - Nạp tiền vào ví
 * - Xem lịch sử giao dịch ví (phân trang và lọc)
 */
public class WalletService {

    private static final BigDecimal MIN_TOPUP_AMOUNT = BigDecimal.valueOf(10000L);
    private static final BigDecimal MAX_TOPUP_AMOUNT = BigDecimal.valueOf(10000000L);

    private final WalletDAO walletDAO = new WalletDAO();
    private final WalletTransactionDAO txDAO = new WalletTransactionDAO();

    // Lấy ví của user, nếu chưa có thì tự động tạo mới ví rỗng.
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

    //1. GET /wallet: Lấy thông tin ví và số dư của người dùng hiện tại
    public WalletResponse getMyWallet(long userId) {
        Wallet wallet = getOrCreateWallet(userId);

        if (wallet.getStatus() == WalletStatus.SUSPENDED) {
            throw WalletException.walletSuspended();
        }

        return new WalletResponse(
                wallet.getId(),
                wallet.getUser() != null ? wallet.getUser().getId() : userId,
                wallet.getBalance(),
                wallet.getStatus()
        );
    }

    // 2. POST /wallet/top-up: Nạp tiền vào ví
    public TopUpResponse topUp(long userId, TopUpRequest req) {
        if (req == null || req.getAmount() == null) {
            throw WalletException.invalidAmount("Số tiền nạp không được để trống");
        }

        BigDecimal amount = BigDecimal.valueOf(req.getAmount());

        // Validate số tiền: tối thiểu 10.000 VND, tối đa 10.000.000 VND
        if (amount.compareTo(MIN_TOPUP_AMOUNT) < 0) {
            throw WalletException.invalidAmount("So tien nap toi thieu 10.000 VND");
        }
        if (amount.compareTo(MAX_TOPUP_AMOUNT) > 0) {
            throw WalletException.invalidAmount("So tien nap toi da 10.000.000 VND");
        }

        // Validate phương thức thanh toán: QR_CODE, MOMO, VNPAY
        String method = req.getMethod() != null ? req.getMethod().trim().toUpperCase() : "";
        if (!"QR_CODE".equals(method) && !"MOMO".equals(method) && !"VNPAY".equals(method)) {
            throw WalletException.invalidMethod("Phuong thuc thanh toan khong hop le");
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
                wallet.setBalance(BigDecimal.ZERO);
                wallet.setStatus(WalletStatus.ACTIVE);
                em.persist(wallet);
            }

            // Chặn giao dịch nếu ví đang bị tạm khóa
            if (wallet.getStatus() == WalletStatus.SUSPENDED) {
                throw WalletException.walletSuspended();
            }

            // Cập nhật số dư ví
            BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
            wallet.setBalance(currentBalance.add(amount));
            em.merge(wallet);

            // Ghi nhận giao dịch ADD_MONEY vào bảng wallet_transactions
            WalletTransaction tx = new WalletTransaction();
            tx.setWallet(wallet);
            tx.setType(TransactionType.ADD_MONEY);
            tx.setStatus(TransactionStatus.SUCCESSFUL);
            tx.setAmount(amount);
            tx.setDescription("Nap tien vao vi qua " + method);
            em.persist(tx);

            em.getTransaction().commit();

            Instant now = Instant.now();
            return new TopUpResponse(
                    tx.getId(),
                    amount,
                    method,
                    "SUCCESSFUL",
                    tx.getId(),
                    now.toString()
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

    // 3. GET /wallet/transactions: Lịch sử bút toán ví
    public HistoryResult getTransactionHistory(long userId, String typeStr, String fromStr, String toStr, int page, int size) {
        Wallet wallet = getOrCreateWallet(userId);

        if (wallet.getStatus() == WalletStatus.SUSPENDED) {
            throw WalletException.walletSuspended();
        }

        if (typeStr != null && !typeStr.isBlank()) {
            String t = typeStr.trim().toUpperCase();
            if (!"ADD_MONEY".equals(t) && !"PAYMENT".equals(t) && !"REFUND".equals(t)) {
                throw WalletException.invalidFilter("Tham so loc type khong hop le (ADD_MONEY, PAYMENT, REFUND)");
            }
        }

        Instant from = null;
        if (fromStr != null && !fromStr.isBlank()) {
            try {
                from = LocalDate.parse(fromStr.trim()).atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException e) {
                throw WalletException.invalidFilter("Định dạng ngày bắt đầu không hợp lệ (YYYY-MM-DD)");
            }
        }

        Instant to = null;
        if (toStr != null && !toStr.isBlank()) {
            try {
                to = LocalDate.parse(toStr.trim()).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException e) {
                throw WalletException.invalidFilter("Định dạng ngày kết thúc không hợp lệ (YYYY-MM-DD)");
            }
        }

        if (from != null && to != null && from.isAfter(to)) {
            throw WalletException.invalidFilter("Ngày bắt đầu không được lớn hơn ngày kết thúc");
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
                    t.getId(),
                    t.getWallet() != null ? t.getWallet().getId() : wallet.getId(),
                    t.getAmount(),
                    t.getType(),
                    t.getStatus(),
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
