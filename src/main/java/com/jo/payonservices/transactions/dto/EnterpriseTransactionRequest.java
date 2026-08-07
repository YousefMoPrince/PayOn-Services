package com.jo.payonservices.transactions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
public class EnterpriseTransactionRequest {
    private BigDecimal amount;
    private String idempotencyKey;
    private Long receiverAccountNumber;
}
