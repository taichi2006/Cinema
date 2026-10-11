package com.cinema.wallet.service;

import com.cinema.common.dto.PageMeta;
import com.cinema.common.dto.PageResponse;
import com.cinema.common.exception.ApiException;
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

import java.time.Instant;
import java.time.LocalDate;
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

    // 3. Nạp tiền vào ví
    public TopUpResponse topUp(long userId, TopUpRequest req) {
        if (req == null || req.getAmount() == null) {
            throw ApiException.badRequest("Số tiền nạp không được để trống");
        }
        if (req.getAmount() < 10000L || req.getAmount() > 50000000L) {
            throw ApiException.badRequest("Số tiền nạp phải từ 10,000 đến 50,000,000 VND");
        }

        WalletTransaction tx = walletDAO.topUp(userId, req.getAmount());
        String method = (req.getMethod() != null && !req.getMethod().isBlank()) ? req.getMethod().trim() : "BANK_TRANSFER";
        return new TopUpResponse(null, tx.getAmount(), method, tx.getStatus().name(), tx.getId(), Instant.now().toString());
    }

    // 4. Kiểm tra trạng thái giao dịch nạp tiền
    public TopUpResponse getTopUpStatus(long userId, long topupId) {
        WalletTransaction tx = txDAO.findById(topupId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch nạp tiền #" + topupId));

        if (tx.getWallet().getUser().getId() != userId || tx.getType() != TransactionType.ADD_MONEY) {
            throw ApiException.notFound("Không tìm thấy giao dịch nạp tiền #" + topupId);
        }

        String created = tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : Instant.now().toString();
        return new TopUpResponse(null, tx.getAmount(), "GATEWAY", tx.getStatus().name(), tx.getId(), created);
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
