package com.jo.payonservices.onpay.service;

import com.jo.payonservices.auth.model.users;
import com.jo.payonservices.auth.repository.UserRepo;
import com.jo.payonservices.merchant.model.Merchant;
import com.jo.payonservices.merchant.repository.MerchantRepo;
import com.jo.payonservices.notifications.service.NotificationService;
import com.jo.payonservices.onpay.dto.ReceiptResponse;
import com.jo.payonservices.onpay.dto.ThirdPartyTransactionApiResponse;
import com.jo.payonservices.onpay.event.PaymentSuccessWebhookEvent;
import com.jo.payonservices.onpay.dto.ThirdPartyTransactionRequest;
import com.jo.payonservices.onpay.dto.ThirdPartyTransactionResponse;
import com.jo.payonservices.onpay.model.OnPay;
import com.jo.payonservices.onpay.model.ThirdPartyStatus;
import com.jo.payonservices.onpay.repository.OnPayRepo;
import com.jo.payonservices.onpay.util.CodeUtilization;
import com.jo.payonservices.wallet.model.Wallet;
import com.jo.payonservices.wallet.repository.WalletRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.SequencedCollection;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThirdPartyTransactionService {

    private final OnPayRepo onpayrepo;
    private final MerchantRepo merchantRepo;
    private final WalletRepo walletRepo;
    private final UserRepo userRepo;
    private final CodeUtilization codeUtilization;
    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    private static final String REDIS_TX_PREFIX = "thptx:idempotency:";

    @Transactional
    public ThirdPartyTransactionResponse newReceipt(ThirdPartyTransactionRequest request) {
        log.info("Processing new receipt webhook for merchant account: {}", request.getData().getAccountNumber());

        Merchant merchant = merchantRepo.findByAccountNumber(request.getData().getAccountNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found"));

        Wallet wallet = walletRepo.findByAccountNumber(request.getData().getAccountNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant Wallet not found"));

        if (!ThirdPartyStatus.PENDING.name().equalsIgnoreCase(request.getState())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ThirdParty transaction state must be PENDING");
        }

        if (request.getData().getAmount() == null || request.getData().getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amount");
        }

        UUID idempotencyKey = request.getData().getIdempotencyKey();

        String Key = REDIS_TX_PREFIX + idempotencyKey.toString();
        Boolean isAbsent = stringRedisTemplate.opsForValue().setIfAbsent(Key, "Processing", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isAbsent)) {
            log.warn("Duplicate request detected for idempotency key: {}", idempotencyKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction is already being processed or has been processed");
        }

        try {
            if (onpayrepo.findByIdempotencyKey(idempotencyKey).isPresent() ||
                    onpayrepo.findByReferenceKey(request.getData().getReferenceKey()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Receipt with this Reference Key or Idempotency Key already exists");
            }

            Long transactionCode = codeUtilization.getTransactionCode();

            OnPay onPay = new OnPay();
            onPay.setMerchantUserName(merchant.getMerchantUserName());
            onPay.setAccountNumber(wallet.getAccountNumber());
            onPay.setTransactionCode(transactionCode);
            onPay.setStatus(request.getState());
            onPay.setAmount(request.getData().getAmount());
            onPay.setCurrency(request.getData().getCurrency() != null ? request.getData().getCurrency() : "EGP");
            onPay.setReferenceKey(request.getData().getReferenceKey());
            onPay.setIdempotencyKey(idempotencyKey);
            onPay.setCreatedAt(LocalDateTime.now());
            OnPay savedTransaction = onpayrepo.save(onPay);
            log.info("Receipt registered successfully with internal ID: {} and transaction code: {}",
                    savedTransaction.getThirdPartyTransactionId(), savedTransaction.getTransactionCode());

            ThirdPartyTransactionResponse.TransactionDataResponse dataResponse =
                    ThirdPartyTransactionResponse.TransactionDataResponse.builder()
                            .transactionId(savedTransaction.getThirdPartyTransactionId())
                            .accountNumber(savedTransaction.getAccountNumber())
                            .referenceKey(savedTransaction.getReferenceKey())
                            .transactionCode(savedTransaction.getTransactionCode())
                            .idempotencyKey(savedTransaction.getIdempotencyKey())
                            .build();

            return ThirdPartyTransactionResponse.builder()
                    .event(ThirdPartyStatus.PENDING.name())
                    .data(dataResponse)
                    .build();

        } catch (Exception e) {
            stringRedisTemplate.delete(Key);
            log.error("Failed to process transaction, removed from Redis lock. Error: {}", e.getMessage());
            throw e;
        }
    }
    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(Long transactionCode) {
        OnPay receipt = onpayrepo.findByTransactionCode(transactionCode).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found"));
        return new ReceiptResponse(receipt.getThirdPartyTransactionId()
                ,receipt.getMerchantUserName()
                ,receipt.getAmount()
                ,receipt.getCurrency()
                ,receipt.getTransactionCode());
    }
    @Transactional
    public void processReceipt(Authentication auth,Long transactionsCode) {
        String userName = (String) auth.getPrincipal();
        OnPay receipt = onpayrepo.findByTransactionCodeForUpdate(transactionsCode).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found"));
        users user = userRepo.findByUserName(userName).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Wallet payerWallet = walletRepo.findByUser_UserId(user.getUserId()).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Payer Wallet not found"));
        Wallet merchantWallet = walletRepo.findByAccountNumber(receipt.getAccountNumber()).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant Wallet not found"));
        Merchant merchant = merchantRepo.findByMerchantUserName(receipt.getMerchantUserName())
                .orElseThrow(() -> new IllegalStateException("Merchant not found for this invoice"));
        if (!ThirdPartyStatus.PENDING.name().equalsIgnoreCase(receipt.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receipt is already processed or cancelled");
        }
        if (payerWallet.getBalance().compareTo(receipt.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Insufficient balance");
        }
            payerWallet.setBalance(payerWallet.getBalance().subtract(receipt.getAmount()));
            merchantWallet.setBalance(merchantWallet.getBalance().add(receipt.getAmount()));
            walletRepo.save(merchantWallet);
            walletRepo.save(payerWallet);
            receipt.setPayerAccountNumber(payerWallet.getAccountNumber());
            receipt.setStatus(ThirdPartyStatus.SUCCESS.name());
            receipt.setUpdatedAt(LocalDateTime.now());
            onpayrepo.save(receipt);
            PaymentSuccessWebhookEvent webhookEvent = new PaymentSuccessWebhookEvent(
                    receipt.getThirdPartyTransactionId(),
                    merchant.getMerchantUrl(),
                    payerWallet.getAccountNumber(),
                    receipt.getReferenceKey(),
                    receipt.getAmount(),
                    receipt.getCurrency()
            );

            eventPublisher.publishEvent(webhookEvent);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationService.sendNotification(
                            payerWallet.getUser().getUserName(),
                            "Success",
                            "Successful Payment",
                            "You have successfully Payment EGP:" + receipt.getAmount() + " to " + merchantWallet.getUser().getUserName() +"."
                    );
                    notificationService.sendNotification(
                            merchantWallet.getUser().getUserName(),
                            "Success",
                            "Successful Payment",
                            "You have successfully been Payment EGP:" + receipt.getAmount() + " from " + payerWallet.getUser().getUserName() +"."
                    );
                }
            });
        } else {
            notificationService.sendNotification(payerWallet.getUser().getUserName(), "Success", "Successful Payment", "You have successfully Payment EGP:" + receipt.getAmount() + " to " + merchantWallet.getUser().getUserName() +".");
            notificationService.sendNotification(merchantWallet.getUser().getUserName(), "Success", "Successful Payment", "You have been successfully Payment EGP:" + receipt.getAmount() + " from " + payerWallet.getUser().getUserName() +".");
        }
    }
    @Transactional
    public ThirdPartyTransactionApiResponse<List<ReceiptResponse>> getReceipts(Authentication auth) {
        String userName = (String) auth.getPrincipal();
        users user = userRepo.findByUserName(userName).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Wallet wallet = walletRepo.findByUser_UserId(user.getUserId()).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        List<OnPay> receipts = onpayrepo.findAllUserTransactions(wallet.getAccountNumber());
        List<ReceiptResponse> responses = receipts.stream().map(re->
                {
                    return ReceiptResponse
                            .builder()
                            .transactionId(re.getThirdPartyTransactionId())
                            .merchantUserName(re.getMerchantUserName())
                            .amount(re.getAmount())
                            .currency(re.getCurrency())
                            .transactionCode(re.getTransactionCode())
                            .build();
                }

        ).collect(Collectors.toList());
        return new ThirdPartyTransactionApiResponse<>("All Receipts Retrieved",responses);
    }

}