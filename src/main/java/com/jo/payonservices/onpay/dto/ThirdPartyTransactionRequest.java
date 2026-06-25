package com.jo.payonservices.onpay.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ThirdPartyTransactionRequest {
    private String state;
    private TransactionDataRequest data;
    @Data
    public static class TransactionDataRequest {
        private BigDecimal amount;
        private Long accountNumber;
        private String currency;
        private String referenceKey;
        private UUID idempotencyKey;
    }
}
