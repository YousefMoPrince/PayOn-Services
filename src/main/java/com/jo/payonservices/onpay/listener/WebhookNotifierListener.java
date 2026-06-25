package com.jo.payonservices.onpay.listener;

import com.jo.payonservices.onpay.event.PaymentSuccessWebhookEvent;
import jakarta.persistence.Column;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookNotifierListener {
    private final RestClient restClient = RestClient.create();
    @Async
    @TransactionalEventListener
    public void handlePaymentSuccessEvent(PaymentSuccessWebhookEvent event){
        Map<String, Object> payload = Map.of(
                "event", "SUCCESS",
                "data", Map.of(
                        "transactionId", event.getTransactionId(),
                        "payerAccountNumber", event.getPayerAccountNumber(),
                        "referenceKey", event.getReferenceKey(),
                        "amount", event.getAmount(),
                        "currency", event.getCurrency()
                )

        );
        try {
            restClient.post()
                    .uri(event.getMerchantUrl())
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to deliver webhook to {}. Error: {}", event.getMerchantUrl(), e.getMessage());
        }
    }
}
