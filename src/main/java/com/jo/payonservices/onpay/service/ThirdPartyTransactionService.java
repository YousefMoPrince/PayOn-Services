package com.jo.payonservices.onpay.service;

import com.jo.payonservices.auth.model.UserStatus;
import com.jo.payonservices.auth.model.users;
import com.jo.payonservices.auth.repository.UserRepo;
import com.jo.payonservices.merchant.model.Merchant;
import com.jo.payonservices.merchant.model.MerchantType;
import com.jo.payonservices.merchant.repository.MerchantRepo;
import com.jo.payonservices.notifications.model.NotificationLog;
import com.jo.payonservices.notifications.repository.NotificationLogRepo;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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
    private final PasswordEncoder passwordEncoder;
    private final NotificationLogRepo notificationLogRepo;

    private static final String REDIS_TX_PREFIX = "thptx:idempotency:";

    public Boolean isValidAccountNumber(Long accountNumber, String email) {
        Wallet wallet = walletRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid account number"));
        users user = userRepo.findByUserName(wallet.getUser().getUserName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid user"));
        return wallet.getAccountNumber().equals(accountNumber) && user.getEmail().equals(email);
    }
    @Transactional
    public ThirdPartyTransactionResponse newReceipt(Authentication auth,ThirdPartyTransactionRequest request) {
        String userName = auth.getName();
        users user = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Merchant merchant = merchantRepo.findByUser_UserName(user.getUserName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found"));
        Wallet wallet = walletRepo.findByAccountNumber(merchant.getAccountNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant Wallet not found"));
        if (!user.getEmail().equals(merchant.getUser().getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not belong to Merchant");
        }
        if (!ThirdPartyStatus.PENDING.equals(request.getState())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ThirdParty transaction state must be PENDING");
        }

        if (request.getData().getAmount() == null || request.getData().getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amount");
        }

        UUID idempotencyKey = request.getData().getIdempotencyKey();
        String referenceKey = "REF-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();        String Key = REDIS_TX_PREFIX + idempotencyKey;
        Boolean isAbsent = stringRedisTemplate.opsForValue().setIfAbsent(Key, "Processing", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isAbsent)) {
            log.warn("Duplicate request detected for idempotency key: {}", idempotencyKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction is already being processed or has been processed");
        }

        try {
            if (onpayrepo.findByIdempotencyKey(idempotencyKey).isPresent() ||
                    onpayrepo.findByReferenceKey(referenceKey).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Receipt with this Reference Key or Idempotency Key already exists");
            }

            Long transactionCode = codeUtilization.getTransactionCode();

            OnPay onPay = new OnPay();
            onPay.setMerchantUserName(merchant.getMerchantUserName());
            onPay.setClientName(request.getData().getClientName());
            onPay.setAccountNumber(wallet.getAccountNumber());
            onPay.setMerchantType(MerchantType.INDIVIDUAL);
            onPay.setTransactionCode(transactionCode);
            onPay.setStatus(request.getState());
            onPay.setAmount(request.getData().getAmount());
            onPay.setCurrency(request.getData().getCurrency() != null ? request.getData().getCurrency() : "EGP");
            onPay.setReferenceKey(referenceKey);
            onPay.setIdempotencyKey(idempotencyKey);
            onPay.setCreatedAt(LocalDateTime.now());
            OnPay savedTransaction = onpayrepo.save(onPay);
            log.info("Receipt registered successfully with internal ID: {} and transaction code: {}",
                    savedTransaction.getThirdPartyTransactionId(), savedTransaction.getTransactionCode());
            ThirdPartyTransactionResponse.TransactionDataResponse dataResponse =
                    ThirdPartyTransactionResponse.TransactionDataResponse.builder()
                            .transactionId(savedTransaction.getThirdPartyTransactionId())
                            .clientName(savedTransaction.getClientName())
                            .accountNumber(savedTransaction.getAccountNumber())
                            .transactionCode(savedTransaction.getTransactionCode())
                            .idempotencyKey(savedTransaction.getIdempotencyKey())
                            .build();

            return ThirdPartyTransactionResponse.builder()
                    .event(ThirdPartyStatus.PENDING)
                    .data(dataResponse)
                    .build();

        } catch (Exception e) {
            stringRedisTemplate.delete(Key);
            log.error("Failed to process transaction, removed from Redis lock. Error: {}", e.getMessage());
            throw e;
        }
    }
    @Transactional
    public ThirdPartyTransactionResponse createEnterpriseReceipt(Authentication auth,ThirdPartyTransactionRequest request){
        String userName = auth.getName();
        users user = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
        Wallet wallet = walletRepo.findByUser_UserName(user.getUserName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant Wallet not found"));
        Merchant merchant = merchantRepo.findByAccountNumber(wallet.getAccountNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found"));
        if (!user.getEmail().equals(merchant.getUser().getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not belong to Merchant");
        }
        if (!ThirdPartyStatus.PENDING.equals(request.getState())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ThirdParty transaction state must be PENDING");
        }

        if (request.getData().getAmount() == null || request.getData().getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amount");
        }
        UUID idempotencyKey = request.getData().getIdempotencyKey();
        String referenceKey = "ENT-REF-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String Key = REDIS_TX_PREFIX + idempotencyKey;
        Boolean isAbsent = stringRedisTemplate.opsForValue().setIfAbsent(Key, "Processing", Duration.ofHours(24));
        try {
            if (onpayrepo.findByIdempotencyKey(idempotencyKey).isPresent() ||
                    onpayrepo.findByReferenceKey(referenceKey).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Receipt with this Reference Key or Idempotency Key already exists");
            }

            Long transactionCode = codeUtilization.getTransactionCode();

            OnPay onPay = new OnPay();
            onPay.setMerchantUserName(merchant.getMerchantUserName());
            onPay.setClientName(request.getData().getClientName());
            onPay.setAccountNumber(wallet.getAccountNumber());
            onPay.setMerchantType(MerchantType.ENTERPRISE);
            onPay.setTransactionCode(transactionCode);
            onPay.setStatus(request.getState());
            onPay.setAmount(request.getData().getAmount());
            onPay.setCurrency(request.getData().getCurrency() != null ? request.getData().getCurrency() : "EGP");
            onPay.setReferenceKey(referenceKey);
            onPay.setIdempotencyKey(idempotencyKey);
            onPay.setCreatedAt(LocalDateTime.now());
            OnPay savedTransaction = onpayrepo.save(onPay);
            log.info("Receipt registered successfully with internal ID: {} and transaction code: {}",
                    savedTransaction.getThirdPartyTransactionId(), savedTransaction.getTransactionCode());
            ThirdPartyTransactionResponse.TransactionDataResponse dataResponse =
                    ThirdPartyTransactionResponse.TransactionDataResponse.builder()
                            .transactionId(savedTransaction.getThirdPartyTransactionId())
                            .clientName(savedTransaction.getClientName())
                            .accountNumber(savedTransaction.getAccountNumber())
                            .transactionCode(savedTransaction.getTransactionCode())
                            .idempotencyKey(savedTransaction.getIdempotencyKey())
                            .build();

            return ThirdPartyTransactionResponse.builder()
                    .event(ThirdPartyStatus.PENDING)
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
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void processReceipt(Authentication auth,Long transactionsCode) {
        String userName = (String) auth.getPrincipal();
        OnPay receipt = onpayrepo.findByTransactionCodeForUpdate(transactionsCode).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found"));
        users user = userRepo.findByUserName(userName).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Wallet payerWallet = walletRepo.findByUserIdForUpdate(user.getUserId()).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Payer Wallet not found"));
        Wallet merchantWallet = walletRepo.findByAccountNumberForUpdate(receipt.getAccountNumber()).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant Wallet not found"));
        Merchant merchant = merchantRepo.findByMerchantUserName(receipt.getMerchantUserName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Merchant not found for this receipt"));
        if (!ThirdPartyStatus.PENDING.name().equalsIgnoreCase(String.valueOf(receipt.getStatus()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receipt is already processed or cancelled");
        }
        if (payerWallet.getBalance().compareTo(receipt.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Insufficient balance");
        }
        BigDecimal realAmount = receipt.getAmount();
        if (receipt.getCurrency().equals("USD")){
            realAmount = realAmount.multiply(BigDecimal.valueOf(53));
        }else if (receipt.getCurrency().equals("EUR")){
            realAmount = realAmount.multiply(BigDecimal.valueOf(67));
        }
            payerWallet.setBalance(payerWallet.getBalance().subtract(realAmount));
            merchantWallet.setBalance(merchantWallet.getBalance().add(realAmount));
            walletRepo.save(merchantWallet);
            walletRepo.save(payerWallet);
            receipt.setPayerAccountNumber(payerWallet.getAccountNumber());
            receipt.setStatus(ThirdPartyStatus.SUCCESS);
            receipt.setUpdatedAt(LocalDateTime.now());
            onpayrepo.save(receipt);
        NotificationLog sendorNotificationLog = NotificationLog
                .builder()
                .username(payerWallet.getUser().getUserName())
                .title("Successful Payment")
                .eventType("Success")
                .message("You have successfully Pay EGP:" + receipt.getAmount() + " to " +receipt.getMerchantUserName() +"for receipt "+ receipt.getTransactionCode()+".")
                .isRead(false)
                .timestamp(LocalDateTime.now())
                .build();
        NotificationLog receiverNotificationLog = NotificationLog
                .builder()
                .username(merchantWallet.getUser().getUserName())
                .title("Successful Payment")
                .eventType("Success")
                .message(payerWallet.getUser().getUserName() + " has successfully Pay the receipt " + receipt.getTransactionCode() + " ,for amount EGP:" + receipt.getAmount() + " .")
                .isRead(false)
                .timestamp(LocalDateTime.now())
                .build();
        notificationLogRepo.save(sendorNotificationLog);
        notificationLogRepo.save(receiverNotificationLog);
        if (merchant.getMerchantType().equals(MerchantType.INDIVIDUAL)){
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationService.sendNotification(
                                payerWallet.getUser().getUserName(),
                                sendorNotificationLog.getEventType(),
                                sendorNotificationLog.getTitle(),
                                sendorNotificationLog.getMessage()
                        );
                        if (merchantWallet.getUser().getStatus().equals(UserStatus.ONLINE)) {
                            notificationService.sendNotification(
                                    merchantWallet.getUser().getUserName(),
                                    receiverNotificationLog.getEventType(),
                                    receiverNotificationLog.getTitle(),
                                    receiverNotificationLog.getMessage()
                            );
                        }else {
                            notificationService.notifyMerchantDevice(merchantWallet.getUser().getUserId(), String.valueOf(receipt.getThirdPartyTransactionId()),receiverNotificationLog.getId());
                        }
                    }
                });
            } else {
                notificationService.sendNotification(payerWallet.getUser().getUserName(), sendorNotificationLog.getEventType(), sendorNotificationLog.getTitle(), sendorNotificationLog.getMessage());
                if (merchantWallet.getUser().getStatus().equals(UserStatus.ONLINE)) {
                    notificationService.sendNotification(merchantWallet.getUser().getUserName(), receiverNotificationLog.getEventType(), receiverNotificationLog.getTitle(), receiverNotificationLog.getMessage());
                } else {
                    notificationService.notifyMerchantDevice(merchantWallet.getUser().getUserId(), String.valueOf(receipt.getThirdPartyTransactionId()),receiverNotificationLog.getId());
                }
            }
        }else {
            PaymentSuccessWebhookEvent webhookEvent = new PaymentSuccessWebhookEvent(
                    receipt.getThirdPartyTransactionId(),
                    receipt.getStatus(),
                    merchant.getMerchantUrl(),
                    merchantWallet.getAccountNumber(),
                    payerWallet.getAccountNumber(),
                    receipt.getAmount(),
                    receipt.getCurrency(),
                    receipt.getIdempotencyKey()
            );
            eventPublisher.publishEvent(webhookEvent);
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationService.sendNotification(
                                payerWallet.getUser().getUserName(),
                                sendorNotificationLog.getEventType(),
                                sendorNotificationLog.getTitle(),
                                sendorNotificationLog.getMessage()
                        );
                    }
                });
            } else {
                notificationService.sendNotification(payerWallet.getUser().getUserName(), sendorNotificationLog.getEventType(), sendorNotificationLog.getTitle(), sendorNotificationLog.getMessage());
            }
        }
    }
    @Transactional
    public ThirdPartyTransactionResponse getCreatedReceipt(Authentication auth,String transactionId) {
        String userName = auth.getName();
        users user = userRepo.findByUserName(userName).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Wallet wallet = walletRepo.findByUser_UserName(user.getUserName()).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        UUID id = UUID.fromString(transactionId);
        OnPay fullReceipt = onpayrepo.findByThirdPartyTransactionIdAndAccountNumber(id, wallet.getAccountNumber())
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Third party transaction not found"));
        ThirdPartyTransactionResponse.TransactionDataResponse dataResponse = ThirdPartyTransactionResponse.TransactionDataResponse
                .builder()
                .transactionId(fullReceipt.getThirdPartyTransactionId())
                .clientName(fullReceipt.getClientName())
                .merchantUserName(fullReceipt.getMerchantUserName())
                .merchantType(fullReceipt.getMerchantType())
                .amount(fullReceipt.getAmount())
                .currency(fullReceipt.getCurrency())
                .accountNumber(fullReceipt.getAccountNumber())
                .payerAccountNumber(fullReceipt.getPayerAccountNumber())
                .transactionCode(fullReceipt.getTransactionCode())
                .idempotencyKey(fullReceipt.getIdempotencyKey())
                .createdAt(fullReceipt.getCreatedAt())
                .updatedAt(fullReceipt.getUpdatedAt())
                .build();
        return ThirdPartyTransactionResponse
                .builder()
                .event(fullReceipt.getStatus())
                .data(dataResponse)
                .build();
    }
    @Transactional
    public ThirdPartyTransactionApiResponse<List<ReceiptResponse>> getReceipts(Authentication auth) {
        String userName = auth.getName();
        users user = userRepo.findByUserName(userName).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Wallet wallet = walletRepo.findByUser_UserName(user.getUserName()).orElseThrow(()->
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
    @Transactional
    public List<ThirdPartyTransactionResponse> getEnterpriseReceipts(Authentication auth) {
        String userName = auth.getName();
        users user = userRepo.findByUserName(userName).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Wallet wallet = walletRepo.findByUser_UserName(user.getUserName()).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        Merchant merchant = merchantRepo.findByAccountNumber(wallet.getAccountNumber())
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found"));
        List<OnPay> receipts = onpayrepo.findAllUserTransactionsByAccountNumberAndMerchantType(merchant.getAccountNumber(),merchant.getMerchantType());
        List<ThirdPartyTransactionResponse> responses = receipts.stream().map(re->
                {
                    ThirdPartyTransactionResponse.TransactionDataResponse data =
                            ThirdPartyTransactionResponse.TransactionDataResponse.builder()
                                    .transactionId(re.getThirdPartyTransactionId())
                                    .clientName(re.getMerchantUserName())
                                    .merchantType(re.getMerchantType())
                                    .accountNumber(re.getAccountNumber())
                                    .amount(re.getAmount())
                                    .payerAccountNumber(re.getPayerAccountNumber())
                                    .merchantUserName(re.getMerchantUserName())
                                    .updatedAt(re.getUpdatedAt())
                                    .createdAt(re.getCreatedAt())
                                    .transactionCode(re.getTransactionCode())
                                    .build();
                    return ThirdPartyTransactionResponse
                            .builder()
                            .event(re.getStatus())
                            .data(data)
                            .build();
                }

        ).collect(Collectors.toList());
        return responses;
    }
    @Transactional
    public List<ThirdPartyTransactionResponse> getMerchantReceipts(Authentication auth) {
        String userName = auth.getName();
        users user = userRepo.findByUserName(userName).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Wallet wallet = walletRepo.findByUser_UserName(user.getUserName()).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        Merchant merchant = merchantRepo.findByAccountNumber(wallet.getAccountNumber())
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found"));
        List<OnPay> receipts = onpayrepo.findAllUserTransactionsByAccountNumberAndMerchantType(merchant.getAccountNumber(),merchant.getMerchantType());
        List<ThirdPartyTransactionResponse> responses = receipts.stream().map(re->
                {
                    ThirdPartyTransactionResponse.TransactionDataResponse data =
                            ThirdPartyTransactionResponse.TransactionDataResponse.builder()
                                    .transactionId(re.getThirdPartyTransactionId())
                                    .clientName(re.getClientName())
                                    .merchantType(re.getMerchantType())
                                    .accountNumber(re.getAccountNumber())
                                    .amount(re.getAmount())
                                    .payerAccountNumber(re.getPayerAccountNumber())
                                    .merchantUserName(re.getMerchantUserName())
                                    .currency(re.getCurrency())
                                    .updatedAt(re.getUpdatedAt())
                                    .createdAt(re.getCreatedAt())
                                    .transactionCode(re.getTransactionCode())
                                    .idempotencyKey(re.getIdempotencyKey())
                                    .build();
                    return ThirdPartyTransactionResponse
                            .builder()
                            .event(re.getStatus())
                            .data(data)
                            .build();
                }

        ).collect(Collectors.toList());
        return responses;
    }

}