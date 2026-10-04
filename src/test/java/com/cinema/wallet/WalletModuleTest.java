package com.cinema.wallet;

import com.cinema.user.User;
import com.cinema.wallet.dto.envelope.ErrorEnvelope;
import com.cinema.wallet.dto.envelope.SuccessEnvelope;
import com.cinema.wallet.dto.response.WalletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

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
    void testWalletDTOEnvelopeSerialization() throws Exception {
        WalletResponse walletResp = new WalletResponse(
                "1",
                new BigDecimal("150000.00"),
                "VND",
                Instant.now().toString()
        );

        SuccessEnvelope<WalletResponse> envelope =
                SuccessEnvelope.of(walletResp, "req-test-trace-id");

        String jsonStr = json.writeValueAsString(envelope);
        assertTrue(jsonStr.contains("\"success\":true"));
        assertTrue(jsonStr.contains("\"currency\":\"VND\""));
        assertTrue(jsonStr.contains("\"traceId\":\"req-test-trace-id\""));
    }

    @Test
    void testWalletDTOErrorEnvelope() throws Exception {
        WalletException ex = WalletException.idempotencyKeyRequired();
        ErrorEnvelope errEnv = ErrorEnvelope.of(
                ex.getCode(),
                ex.getMessage(),
                ex.getFieldErrors(),
                "req-err-trace"
        );

        String jsonStr = json.writeValueAsString(errEnv);
        assertTrue(jsonStr.contains("\"success\":false"));
        assertTrue(jsonStr.contains("\"code\":\"IDEMPOTENCY_KEY_REQUIRED\""));
        assertTrue(jsonStr.contains("idempotencyKey"));
    }
}
