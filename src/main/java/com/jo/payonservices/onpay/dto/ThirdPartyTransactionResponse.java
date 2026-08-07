package com.jo.payonservices.onpay.dto;

import com.jo.payonservices.merchant.model.MerchantType;
import com.jo.payonservices.onpay.model.ThirdPartyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThirdPartyTransactionResponse{
    private ThirdPartyStatus event;
    private TransactionDataResponse data;
    @Data
    @Builder
    public static class TransactionDataResponse{
        private UUID transactionId;
        private String clientName;
        private String merchantUserName;
        private MerchantType merchantType;
        private BigDecimal amount;
        private Long accountNumber;
        private Long payerAccountNumber;
        private Long transactionCode;
        private UUID idempotencyKey;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

    }
}
