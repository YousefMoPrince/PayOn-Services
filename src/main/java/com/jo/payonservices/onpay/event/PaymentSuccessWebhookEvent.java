package com.jo.payonservices.onpay.event;

import com.jo.payonservices.onpay.model.ThirdPartyStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class PaymentSuccessWebhookEvent {
    private UUID transactionId;
    private ThirdPartyStatus status;
    private String merchantUrl;
    private Long merchantAccountNumber;
    private Long payerAccountNumber;
    private BigDecimal amount;
    private String currency;
    private UUID idempotencyKey;
}
