package com.cinema.wallet;

import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.dto.CommonDTO.ErrorResponse;
import com.cinema.common.dto.CommonDTO.PageMeta;
import com.cinema.user.User;
import com.cinema.wallet.dto.response.WalletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WalletModuleTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void testWalletEntityDefaults() {
        User user = new User();
        user.setId(10L);
        user.setEmail("test@cinema.com");

        Wallet wallet = new Wallet(user);
        assertEquals(0L, wallet.getBalance());
        assertEquals("VND", wallet.getCurrency());
        assertEquals(WalletStatus.ACTIVE, wallet.getStatus());
        assertEquals(user, wallet.getUser());
    }

    @Test
    void testWalletTopupEntity() {
        Wallet wallet = new Wallet();
        wallet.setId(1L);

        WalletTopup topup = new WalletTopup();
        topup.setWallet(wallet);
        topup.setAmount(100000L);
        topup.setCurrency("VND");
        topup.setStatus(WalletTopupStatus.PENDING);

        assertEquals(100000L, topup.getAmount());
        assertEquals("VND", topup.getCurrency());
        assertEquals(WalletTopupStatus.PENDING, topup.getStatus());
        assertEquals(wallet, topup.getWallet());
    }

    @Test
    void testWalletTransactionEntity() {
        Wallet wallet = new Wallet();
        wallet.setId(1L);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setTransactionType(TransactionType.TOP_UP.name());
        tx.setDirection(TransactionType.TOP_UP.getDirection());
        tx.setAmount(50000L);
        tx.setBalanceAfter(50000L);
        tx.setCurrency("VND");
        tx.setTopupId(10L);
        tx.setDescription("Nạp tiền ví");

        assertEquals("TOP_UP", tx.getTransactionType());
        assertEquals("CREDIT", tx.getDirection());
        assertEquals(50000L, tx.getAmount());
        assertEquals(50000L, tx.getBalanceAfter());
        assertEquals(10L, tx.getTopupId());
        assertEquals("VND", tx.getCurrency());
    }

    @Test
    void testWalletDTOApiResponseSerialization() throws Exception {
        WalletResponse walletResp = new WalletResponse(
                "1",
                150000L,
                "VND",
                Instant.now().toString()
        );

        ApiResponse<WalletResponse> response = ApiResponse.ok(walletResp);

        String jsonStr = json.writeValueAsString(response);
        assertTrue(jsonStr.contains("\"success\":true"));
        assertTrue(jsonStr.contains("\"currency\":\"VND\""));
        assertTrue(jsonStr.contains("\"balance\":150000"));
    }

    @Test
    void testWalletPaginationSerialization() throws Exception {
        WalletResponse walletResp = new WalletResponse(
                "1",
                150000L,
                "VND",
                Instant.now().toString()
        );

        PageMeta meta = new PageMeta(0, 20, 1L, 1);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", List.of(walletResp));
        result.put("meta", meta);

        String jsonStr = json.writeValueAsString(result);
        assertTrue(jsonStr.contains("\"success\":true"));
        assertTrue(jsonStr.contains("\"page\":0"));
        assertTrue(jsonStr.contains("\"totalElements\":1"));
    }

    @Test
    void testWalletErrorResponseSerialization() throws Exception {
        WalletException ex = WalletException.idempotencyKeyRequired();
        ErrorResponse errResp = new ErrorResponse(ex.getStatus(), ex.getMessage());

        String jsonStr = json.writeValueAsString(errResp);
        assertTrue(jsonStr.contains("\"success\":false"));
        assertTrue(jsonStr.contains("\"status\":400"));
        assertTrue(jsonStr.contains("Idempotency-Key"));
    }
}
