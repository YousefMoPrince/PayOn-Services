package com.jo.payonservices.onpay.dto;

import com.jo.payonservices.onpay.model.ThirdPartyStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class EnterpriseThirdPartyTransactionRequest {
    private ThirdPartyStatus state;
    private EnterpriseTransactionDataRequest data;
    @Data
    public static class EnterpriseTransactionDataRequest {
        private BigDecimal amount;
        private String clientName;
        private Long accountNumber;
        private String currency;
        private String referenceKey;
        private UUID idempotencyKey;
    }
}
