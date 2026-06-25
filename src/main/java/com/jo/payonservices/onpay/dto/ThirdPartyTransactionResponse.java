package com.jo.payonservices.onpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.SequencedCollection;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThirdPartyTransactionResponse{
    private String event;
    private TransactionDataResponse data;
    @Data
    @Builder
    public static class TransactionDataResponse{
        private UUID transactionId;
        private Long accountNumber;
        private String referenceKey;
        private Long transactionCode;
        private UUID idempotencyKey;
    }
}
