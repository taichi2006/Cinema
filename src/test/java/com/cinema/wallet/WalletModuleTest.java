package com.cinema.wallet;

import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.dto.CommonDTO.ErrorResponse;
import com.cinema.common.dto.CommonDTO.PageMeta;
import com.cinema.user.entity.User;
import com.cinema.wallet.dto.response.TransactionItemResponse;
import com.cinema.wallet.dto.response.WalletResponse;
import com.cinema.wallet.entity.Wallet;
import com.cinema.wallet.entity.WalletTransaction;
import com.cinema.wallet.enums.TransactionStatus;
import com.cinema.wallet.enums.TransactionType;
import com.cinema.wallet.enums.WalletStatus;
import com.cinema.wallet.exception.WalletException;
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
        tx.setType(TransactionType.ADD_MONEY);
        tx.setStatus(TransactionStatus.SUCCESSFUL);
        tx.setAmount(BigDecimal.valueOf(50000L));
        tx.setDescription("Nạp tiền ví");

        assertEquals(TransactionType.ADD_MONEY, tx.getType());
        assertEquals(TransactionStatus.SUCCESSFUL, tx.getStatus());
        assertEquals(BigDecimal.valueOf(50000L), tx.getAmount());
        assertEquals("Nạp tiền ví", tx.getDescription());
        assertEquals(wallet, tx.getWallet());
    }

    @Test
    void testWalletDTOApiResponseSerialization() throws Exception {
        WalletResponse walletResp = new WalletResponse(
                1L,
                10L,
                BigDecimal.valueOf(150000L),
                WalletStatus.ACTIVE
        );

        ApiResponse<WalletResponse> response = ApiResponse.ok(walletResp);

        String jsonStr = json.writeValueAsString(response);
        assertTrue(jsonStr.contains("\"success\":true"));
        assertTrue(jsonStr.contains("\"walletId\":1"));
        assertTrue(jsonStr.contains("\"userId\":10"));
        assertTrue(jsonStr.contains("\"balance\":150000"));
        assertTrue(jsonStr.contains("\"status\":\"ACTIVE\""));
    }

    @Test
    void testWalletPaginationSerialization() throws Exception {
        TransactionItemResponse item = new TransactionItemResponse(
                1L,
                1L,
                BigDecimal.valueOf(100000L),
                TransactionType.ADD_MONEY,
                TransactionStatus.SUCCESSFUL,
                "Nạp tiền ví",
                Instant.now().toString()
        );

        PageMeta meta = new PageMeta(0, 20, 1L, 1);
        Map<String, Object> pageData = new LinkedHashMap<>();
        pageData.put("items", List.of(item));
        pageData.put("meta", meta);

        ApiResponse<Map<String, Object>> response = ApiResponse.ok(pageData);

        String jsonStr = json.writeValueAsString(response);
        assertTrue(jsonStr.contains("\"success\":true"));
        assertTrue(jsonStr.contains("\"transactionId\":1"));
        assertTrue(jsonStr.contains("\"transactionType\":\"ADD_MONEY\""));
        assertTrue(jsonStr.contains("\"page\":0"));
        assertTrue(jsonStr.contains("\"totalElements\":1"));
    }

    @Test
    void testWalletErrorResponseSerialization() throws Exception {
        WalletException ex = WalletException.walletSuspended();
        ErrorResponse errResp = new ErrorResponse(ex.getStatus(), ex.getMessage());

        String jsonStr = json.writeValueAsString(errResp);
        assertTrue(jsonStr.contains("\"success\":false"));
        assertTrue(jsonStr.contains("\"status\":403"));
        assertTrue(jsonStr.contains("SUSPENDED"));
    }
}
