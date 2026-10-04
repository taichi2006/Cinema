package com.cinema.wallet;

import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.dto.CommonDTO.ErrorResponse;
import com.cinema.common.dto.CommonDTO.PageMeta;
import com.cinema.user.User;
import com.cinema.wallet.dto.response.WalletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
        assertEquals(BigDecimal.ZERO, wallet.getBalance());
        assertEquals(WalletStatus.ACTIVE, wallet.getStatus());
        assertEquals(user, wallet.getUser());
    }

    @Test
    void testWalletTransactionEntity() {
        Wallet wallet = new Wallet();
        wallet.setId(1L);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setAmount(new BigDecimal("50000.00"));
        tx.setTransactionType(TransactionType.TOP_UP);
        tx.setStatus(TransactionStatus.PENDING);
        tx.setReferenceId("key-1234567890123456");
        tx.setDescription("Nạp tiền ví");

        assertEquals(TransactionType.TOP_UP, tx.getTransactionType());
        assertEquals(TransactionStatus.PENDING, tx.getStatus());
        assertEquals("key-1234567890123456", tx.getReferenceId());
        assertFalse(tx.getStatus().isSuccessful());

        tx.setStatus(TransactionStatus.SUCCEEDED);
        assertTrue(tx.getStatus().isSuccessful());
    }

    @Test
    void testWalletDTOApiResponseSerialization() throws Exception {
        WalletResponse walletResp = new WalletResponse(
                "1",
                new BigDecimal("150000.00"),
                "VND",
                Instant.now().toString()
        );

        ApiResponse<WalletResponse> response = ApiResponse.ok(walletResp);

        String jsonStr = json.writeValueAsString(response);
        assertTrue(jsonStr.contains("\"success\":true"));
        assertTrue(jsonStr.contains("\"currency\":\"VND\""));
        assertTrue(jsonStr.contains("\"balance\":150000.00"));
    }

    @Test
    void testWalletPaginationSerialization() throws Exception {
        WalletResponse walletResp = new WalletResponse(
                "1",
                new BigDecimal("150000.00"),
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

