package com.jo.payonservices.onpay.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class PaymentSuccessWebhookEvent {
    private UUID transactionId;
    private String merchantUrl;
    private Long payerAccountNumber;
    private String referenceKey;
    private BigDecimal amount;
    private String currency;
}
