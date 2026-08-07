package com.jo.payonservices.onpay.listener;

import com.jo.payonservices.merchant.model.Merchant;
import com.jo.payonservices.merchant.repository.MerchantRepo;
import com.jo.payonservices.onpay.event.PaymentSuccessWebhookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookNotifierListener {
    private final RestClient restClient = RestClient.create();
    private final MerchantRepo merchantRepo;
    private final PasswordEncoder passwordEncoder;
    @Async
    @TransactionalEventListener
    public void handlePaymentSuccessEvent(PaymentSuccessWebhookEvent event){
        Merchant merchant = merchantRepo.findByAccountNumber(event.getMerchantAccountNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Merchant not found"));
        Map<String, Object> payload = Map.of(
                "event", event.getStatus(),
                "data", Map.of(
                        "transactionId", event.getTransactionId(),
                        "merchantAccountNumber", event.getMerchantAccountNumber(),
                        "payerAccountNumber", event.getPayerAccountNumber(),
                        "amount", event.getAmount(),
                        "currency", event.getCurrency(),
                        "idempotencyKey",event.getIdempotencyKey()
                )

        );
        String url = event.getMerchantUrl() + "api/web/v1/payment/webhook/receipt/ack";
        try {
            restClient.post()
                    .uri(url)
                    .header("X-PAYON-API-KEY",merchant.getApiKey())
                    .header("X-PAYON-API-SECRET",merchant.getApiSecret())
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to deliver webhook to {}. Error: {}", event.getMerchantUrl(), e.getMessage());
        }
    }
}
