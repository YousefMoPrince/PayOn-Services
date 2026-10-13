package com.jo.payonservices.onpay.dto;

import com.jo.payonservices.onpay.model.ThirdPartyStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ThirdPartyTransactionRequest {
    private ThirdPartyStatus state;
    private TransactionDataRequest data;
    @Data
    public static class TransactionDataRequest {
        private BigDecimal amount;
        private String clientName;
        private String currency;
        private UUID idempotencyKey;
    }
}
