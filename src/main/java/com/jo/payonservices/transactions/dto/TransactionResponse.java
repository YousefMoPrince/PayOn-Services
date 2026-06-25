package com.jo.payonservices.transactions.dto;

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
public class TransactionResponse {
    private UUID transactionId;
    private BigDecimal amount;
    private String type;
    private String receiverUsername;
    private String status;
    private String idempotencyKey;
    private LocalDateTime timestamp;
}
