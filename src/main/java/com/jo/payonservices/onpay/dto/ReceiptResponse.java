package com.jo.payonservices.onpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptResponse {
    private UUID transactionId;
    private String merchantUserName;
    private BigDecimal amount;
    private String currency;
    private Long transactionCode;
}
