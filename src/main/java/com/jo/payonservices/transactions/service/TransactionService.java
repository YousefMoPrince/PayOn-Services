package com.jo.payonservices.transactions.service;

import com.jo.payonservices.auth.model.UserStatus;
import com.jo.payonservices.auth.model.users;
import com.jo.payonservices.auth.repository.UserRepo;
import com.jo.payonservices.merchant.model.EnterpriseMerchantStatus;
import com.jo.payonservices.merchant.model.Merchant;
import com.jo.payonservices.merchant.model.MerchantType;
import com.jo.payonservices.merchant.repository.MerchantRepo;
import com.jo.payonservices.notifications.model.NotificationLog;
import com.jo.payonservices.notifications.repository.NotificationLogRepo;
import com.jo.payonservices.notifications.service.NotificationService;
import com.jo.payonservices.transactions.dto.*;
import com.jo.payonservices.transactions.model.Status;
import com.jo.payonservices.transactions.model.Transaction;
import com.jo.payonservices.transactions.model.TransactionType;
import com.jo.payonservices.transactions.repository.TransactionRepo;
import com.jo.payonservices.wallet.model.Wallet;
import com.jo.payonservices.wallet.repository.WalletRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service

public class TransactionService {
    @Autowired
    private TransactionRepo transactionRepo;
    @Autowired
    private WalletRepo walletRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private MerchantRepo merchantRepo;
    @Autowired
    private NotificationLogRepo notificationLogRepo;

    private static final String REDIS_TX_PREFIX = "tx:idempotency:";
    private static final String REDIS_ACK_PREFIX = "tx:ack:idempotency:";


    public TransactionResponse deposit(Authentication auth, TransactionRequest request) {
        String username = auth.getName();
        String key = REDIS_TX_PREFIX + request.getIdempotencyKey();

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amount");
        }

        Boolean isAbsent = stringRedisTemplate.opsForValue().setIfAbsent(key, "Processing", Duration.ofHours(24));
        if (Boolean.FALSE.equals(isAbsent)) {
            return transactionRepo.findByIdempotencyKey(request.getIdempotencyKey())
                    .map(tx -> TransactionResponse.builder()
                            .transactionId(tx.getTransactionId())
                            .amount(tx.getAmount())
                            .idempotencyKey(tx.getIdempotencyKey())
                            .receiverUsername(null)
                            .status(tx.getStatus().name())
                            .type(tx.getTransactionType().name())
                            .timestamp(tx.getCreatedAt() != null ? tx.getCreatedAt() : LocalDateTime.now())
                            .build())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Transaction is already being processed."));
        }

        final Wallet[] walletHolder = new Wallet[1];
        try {
            return transactionTemplate.execute(status -> {
                users user = userRepo.findByUserName(username)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                walletHolder[0] = walletRepo.findByUserIdForUpdate(user.getUserId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

                walletHolder[0].setBalance(walletHolder[0].getBalance().add(request.getAmount()));
                walletRepo.save(walletHolder[0]);

                Transaction transaction = Transaction.builder()
                        .walletId(walletHolder[0])
                        .receiverWalletId(null)
                        .amount(request.getAmount())
                        .transactionType(TransactionType.DEPOSIT)
                        .status(Status.SUCCESS)
                        .idempotencyKey(request.getIdempotencyKey())
                        .createdAt(LocalDateTime.now())
                        .build();
                transactionRepo.save(transaction);
                stringRedisTemplate.opsForValue().set(key, "SUCCESS", Duration.ofHours(24));
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            notificationService.sendNotification(
                                    user.getUserName(),
                                    "Success",
                                    "Successful Deposit",
                                    "You have successfully Deposit EGP:" + request.getAmount() + "."
                            );                        }
                    });
                } else {
                    notificationService.sendNotification(user.getUserName(), "Success", "Successful Deposit", "You have successfully Deposit EGP:" + request.getAmount() + ".");
                }

                return TransactionResponse.builder()
                        .transactionId(transaction.getTransactionId())
                        .amount(transaction.getAmount())
                        .idempotencyKey(transaction.getIdempotencyKey())
                        .receiverUsername(null)
                        .status(transaction.getStatus().name())
                        .type(transaction.getTransactionType().name())
                        .timestamp(transaction.getCreatedAt())
                        .build();
            });
        } catch (Exception e) {
            stringRedisTemplate.opsForValue().set(key, "FAILED", Duration.ofHours(24));
            if (walletHolder[0] != null) {
                transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                transactionTemplate.execute(status -> transactionRepo.save(Transaction.builder()
                        .walletId(walletHolder[0])
                        .amount(request.getAmount())
                        .receiverWalletId(null)
                        .transactionType(TransactionType.DEPOSIT)
                        .status(Status.FAILED)
                        .idempotencyKey(request.getIdempotencyKey())
                        .createdAt(LocalDateTime.now())
                        .build()));
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            notificationService.sendNotification(
                                    walletHolder[0].getUser().getUserName(),
                                    "FAILED",
                                    "Failed Deposit",
                                    "You haven't deposited EGP:" + request.getAmount() + "."
                            );
                        }
                    });
                } else {
                    notificationService.sendNotification(walletHolder[0].getUser().getUserName(), "FAILED", "Failed Deposit", "You haven't deposited EGP:" + request.getAmount() + ".");
                }
            }
            throw e;
        }
    }


    public TransactionResponse withdraw(Authentication auth, TransactionRequest request) {
        String username = auth.getName();
        String key = REDIS_TX_PREFIX + request.getIdempotencyKey();

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amount");
        }

        Boolean isAbsent = stringRedisTemplate.opsForValue().setIfAbsent(key, "Processing", Duration.ofHours(24));
        if (Boolean.FALSE.equals(isAbsent)) {
            return transactionRepo.findByIdempotencyKey(request.getIdempotencyKey())
                    .map(tx -> TransactionResponse.builder()
                            .transactionId(tx.getTransactionId())
                            .amount(tx.getAmount())
                            .idempotencyKey(tx.getIdempotencyKey())
                            .receiverUsername(null)
                            .status(tx.getStatus().name())
                            .type(tx.getTransactionType().name())
                            .timestamp(tx.getCreatedAt() != null ? tx.getCreatedAt() : LocalDateTime.now())
                            .build())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Transaction is already being processed."));
        }

        final Wallet[] walletHolder = new Wallet[1];
        try {
            return transactionTemplate.execute(status -> {
                users user = userRepo.findByUserName(username)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                walletHolder[0] = walletRepo.findByUserIdForUpdate(user.getUserId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

                if (walletHolder[0].getBalance().compareTo(request.getAmount()) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
                }

                walletHolder[0].setBalance(walletHolder[0].getBalance().subtract(request.getAmount()));
                walletRepo.save(walletHolder[0]);

                Transaction transaction = Transaction.builder()
                        .walletId(walletHolder[0])
                        .receiverWalletId(null)
                        .amount(request.getAmount())
                        .status(Status.SUCCESS)
                        .transactionType(TransactionType.WITHDRAWAL)
                        .idempotencyKey(request.getIdempotencyKey())
                        .createdAt(LocalDateTime.now())
                        .build();

                transactionRepo.save(transaction);
                stringRedisTemplate.opsForValue().set(key, "SUCCESS", Duration.ofHours(24));
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            notificationService.sendNotification(
                                    user.getUserName(),
                                    "Success",
                                    "Successful Withdrawal",
                                    "You have successfully Withdraw EGP:" + request.getAmount() + "."
                            );
                        }
                    });
                } else {
                    notificationService.sendNotification(user.getUserName(), "Success", "Successful Withdrawal", "You have successfully Withdraw EGP:" + request.getAmount() + ".");
                }

                return TransactionResponse.builder()
                        .transactionId(transaction.getTransactionId())
                        .amount(transaction.getAmount())
                        .idempotencyKey(transaction.getIdempotencyKey())
                        .receiverUsername(null)
                        .status(transaction.getStatus().name())
                        .type(transaction.getTransactionType().name())
                        .timestamp(transaction.getCreatedAt())
                        .build();
            });
        } catch (Exception e) {
            stringRedisTemplate.opsForValue().set(key, "FAILED", Duration.ofHours(24));
            if (walletHolder[0] != null) {
                transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                transactionTemplate.execute(status -> transactionRepo.save(Transaction.builder()
                        .walletId(walletHolder[0])
                        .amount(request.getAmount())
                        .receiverWalletId(null)
                        .transactionType(TransactionType.WITHDRAWAL)
                        .status(Status.FAILED)
                        .idempotencyKey(request.getIdempotencyKey())
                        .createdAt(LocalDateTime.now())
                        .build()));
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            notificationService.sendNotification(
                                   walletHolder[0].getUser().getUserName(),
                                    "Failed",
                                    "Failed Withdrawal",
                                    "You haven't Withdraw EGP:" + request.getAmount() + "."
                            );
                        }
                    });
                } else {
                    notificationService.sendNotification(walletHolder[0].getUser().getUserName(), "Failed", "Failed Withdrawal", "You haven't Withdraw EGP:" + request.getAmount() + ".");
                }

            }
            throw e;
        }
    }


    public TransactionResponse transfer(Authentication auth, TransactionRequest request) {
        String username = auth.getName();
        String key = REDIS_TX_PREFIX + request.getIdempotencyKey();

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amount");
        }
        if (username.equals(request.getReceiverUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transfer to yourself");
        }

        Boolean isAbsent = stringRedisTemplate.opsForValue().setIfAbsent(key, "Processing", Duration.ofHours(24));
        if (Boolean.FALSE.equals(isAbsent)) {
            return transactionRepo.findByIdempotencyKey(request.getIdempotencyKey())
                    .map(tx -> TransactionResponse.builder()
                            .transactionId(tx.getTransactionId())
                            .amount(tx.getAmount())
                            .idempotencyKey(tx.getIdempotencyKey())
                            .receiverUsername(request.getReceiverUsername())
                            .status(tx.getStatus().name())
                            .type(tx.getTransactionType().name())
                            .timestamp(tx.getCreatedAt() != null ? tx.getCreatedAt() : LocalDateTime.now())
                            .build())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Transaction is already being processed."));
        }

        final Wallet[] walletHolder = new Wallet[2];
        try {
            return transactionTemplate.execute(status -> {
                users sender = userRepo.findByUserName(username)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender user not found"));
                users receiver = userRepo.findByUserName(request.getReceiverUsername())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver user not found"));

                if (sender.getUserId().compareTo(receiver.getUserId()) < 0) {
                    walletHolder[0] = walletRepo.findByUserIdForUpdate(sender.getUserId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender wallet not found"));
                    walletHolder[1] = walletRepo.findByUserIdForUpdate(receiver.getUserId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver wallet not found"));
                } else {
                    walletHolder[1] = walletRepo.findByUserIdForUpdate(receiver.getUserId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver wallet not found"));
                    walletHolder[0] = walletRepo.findByUserIdForUpdate(sender.getUserId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender wallet not found"));
                }

                if (walletHolder[0].getBalance().compareTo(request.getAmount()) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
                }

                Transaction transaction = Transaction.builder()
                        .walletId(walletHolder[0])
                        .receiverWalletId(walletHolder[1])
                        .amount(request.getAmount())
                        .transactionType(TransactionType.TRANSFER)
                        .status(Status.PENDING)
                        .idempotencyKey(request.getIdempotencyKey())
                        .createdAt(LocalDateTime.now())
                        .build();

                transactionRepo.save(transaction);
                stringRedisTemplate.opsForValue().set(key, "PENDING", Duration.ofHours(24));
                return TransactionResponse.builder()
                        .transactionId(transaction.getTransactionId())
                        .amount(transaction.getAmount())
                        .idempotencyKey(transaction.getIdempotencyKey())
                        .receiverUsername(request.getReceiverUsername())
                        .status(transaction.getStatus().name())
                        .type(transaction.getTransactionType().name())
                        .timestamp(transaction.getCreatedAt())
                        .build();
            });
        } catch (Exception e) {
            stringRedisTemplate.opsForValue().set(key, "FAILED", Duration.ofHours(24));
            if (walletHolder[0] != null) {
                transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                transactionTemplate.execute(status -> transactionRepo.save(Transaction.builder()
                        .walletId(walletHolder[0])
                        .receiverWalletId(walletHolder[1])
                        .amount(request.getAmount())
                        .transactionType(TransactionType.TRANSFER)
                        .status(Status.FAILED)
                        .idempotencyKey(request.getIdempotencyKey())
                        .createdAt(LocalDateTime.now())
                        .build()));
            }
            throw e;
        }
    }


    public TransactionResponse transferAck(String idempotencyKey, Status state) {
        String ackKey = REDIS_ACK_PREFIX + idempotencyKey;
        String txKey = REDIS_TX_PREFIX + idempotencyKey;

        Transaction transaction = transactionRepo.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (transaction.getStatus() != Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction is already finalized with status: " + transaction.getStatus());
        }

        Boolean isAbsent = stringRedisTemplate.opsForValue().setIfAbsent(ackKey, "Processing", Duration.ofHours(24));
        if (Boolean.FALSE.equals(isAbsent)) {
            String targetUser = transaction.getReceiverWalletId().getUser().getUserName();
            return transactionRepo.findByIdempotencyKey(idempotencyKey)
                    .map(tx -> TransactionResponse.builder()
                            .transactionId(tx.getTransactionId())
                            .amount(tx.getAmount())
                            .idempotencyKey(tx.getIdempotencyKey())
                            .receiverUsername(targetUser)
                            .status(tx.getStatus().name())
                            .type(tx.getTransactionType().name())
                            .timestamp(tx.getCreatedAt() != null ? tx.getCreatedAt() : LocalDateTime.now())
                            .build())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Transaction is already being processed."));
        }

        try {
            return transactionTemplate.execute(status -> {
                Wallet senderWallet = walletRepo.findByUserIdForUpdate(transaction.getWalletId().getUser().getUserId()).get();
                Wallet receiverWallet = walletRepo.findByUserIdForUpdate(transaction.getReceiverWalletId().getUser().getUserId()).get();

                if (state == Status.SUCCESS) {
                    if (senderWallet.getBalance().compareTo(transaction.getAmount()) < 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance for confirmation");
                    }
                    senderWallet.setBalance(senderWallet.getBalance().subtract(transaction.getAmount()));
                    receiverWallet.setBalance(receiverWallet.getBalance().add(transaction.getAmount()));

                    walletRepo.save(senderWallet);
                    walletRepo.save(receiverWallet);

                    transaction.setStatus(Status.SUCCESS);
                } else {
                    transaction.setStatus(Status.CANCELLED);
                }

                transaction.setCreatedAt(LocalDateTime.now());
                transactionRepo.save(transaction);

                stringRedisTemplate.opsForValue().set(txKey, transaction.getStatus().name(), Duration.ofHours(24));
                stringRedisTemplate.opsForValue().set(ackKey, "SUCCESS", Duration.ofHours(24));
                NotificationLog sendorNotificationLog = NotificationLog
                        .builder()
                        .username(senderWallet.getUser().getUserName())
                        .title("Successful Transfer")
                        .eventType("Success")
                        .message("You have successfully Transferred EGP:" + transaction.getAmount() + " to " + receiverWallet.getUser().getUserName() +".")
                        .isRead(false)
                        .timestamp(LocalDateTime.now())
                        .build();
                NotificationLog receiverNotificationLog = NotificationLog
                        .builder()
                        .username(receiverWallet.getUser().getUserName())
                        .title("Successful Transfer")
                        .eventType("Success")
                        .message("You have successfully been Transferred EGP:" + transaction.getAmount() + " from " + senderWallet.getUser().getUserName() +".")
                        .isRead(false)
                        .timestamp(LocalDateTime.now())
                        .build();
                notificationLogRepo.save(sendorNotificationLog);
                notificationLogRepo.save(receiverNotificationLog);
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            notificationService.sendNotification(
                                    senderWallet.getUser().getUserName(),
                                    sendorNotificationLog.getEventType(),
                                    sendorNotificationLog.getTitle(),
                                    sendorNotificationLog.getMessage()
                            );
                            if (receiverWallet.getUser().getStatus().equals(UserStatus.ONLINE)){
                            notificationService.sendNotification(
                                    receiverWallet.getUser().getUserName(),
                                    receiverNotificationLog.getEventType(),
                                    receiverNotificationLog.getTitle(),
                                    receiverNotificationLog.getMessage()
                            );
                            }else {
                                notificationService.notifyUserDevice(receiverWallet.getUser().getUserId(), String.valueOf(transaction.getTransactionId()),receiverNotificationLog.getId());
                            }
                        }
                    });
                } else {
                    notificationService.sendNotification(senderWallet.getUser().getUserName(), sendorNotificationLog.getEventType(), sendorNotificationLog.getTitle(), sendorNotificationLog.getMessage());
                    if (receiverWallet.getUser().getStatus().equals(UserStatus.ONLINE)) {
                        notificationService.sendNotification(receiverWallet.getUser().getUserName(), receiverNotificationLog.getEventType(), receiverNotificationLog.getTitle(), receiverNotificationLog.getMessage());
                    }else {
                        notificationService.notifyUserDevice(receiverWallet.getUser().getUserId(), String.valueOf(transaction.getTransactionId()),receiverNotificationLog.getId());
                    }
                }
                return TransactionResponse.builder()
                        .transactionId(transaction.getTransactionId())
                        .amount(transaction.getAmount())
                        .idempotencyKey(transaction.getIdempotencyKey())
                        .receiverUsername(receiverWallet.getUser().getUserName())
                        .status(transaction.getStatus().name())
                        .type(transaction.getTransactionType().name())
                        .timestamp(transaction.getCreatedAt())
                        .build();
            });
        } catch (Exception e) {
            stringRedisTemplate.opsForValue().set(ackKey, "FAILED", Duration.ofHours(24));
            throw e;
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public EnterpriseTransactionResponse enterpriseTransfer(Authentication auth, EnterpriseTransactionRequest request) {
        String userName = auth.getName();
        Merchant merchant = merchantRepo.findByUser_UserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found"));
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amount");
        }

        if (merchant.getAccountNumber().equals(request.getReceiverAccountNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transfer to yourself");
        }
        String key = REDIS_TX_PREFIX + request.getIdempotencyKey();
        Boolean isAbsent = stringRedisTemplate.opsForValue().setIfAbsent(key, "Processing", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isAbsent)) {
            String currentStatus = stringRedisTemplate.opsForValue().get(key);
            if ("Processing".equals(currentStatus)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction is currently being processed. Please wait.");
            }
            if ("SUCCESS".equals(currentStatus)) {
                return transactionRepo.findByIdempotencyKey(request.getIdempotencyKey())
                        .map(tx -> EnterpriseTransactionResponse.builder()
                                .transactionId(tx.getTransactionId())
                                .amount(tx.getAmount())
                                .idempotencyKey(tx.getIdempotencyKey())
                                .receiverAccountNumber(request.getReceiverAccountNumber())
                                .status(tx.getStatus().name())
                                .type(tx.getTransactionType().name())
                                .timestamp(tx.getCreatedAt() != null ? tx.getCreatedAt() : LocalDateTime.now())
                                .build())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Transaction is already being processed."));
            }
            if ("FAILED".equals(currentStatus)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "This idempotency key belongs to a previously failed transaction.");
            }
        }
        final Wallet[] walletHolder = new Wallet[2];
        try {

                walletHolder[0] = walletRepo.findByUserIdForUpdate(merchant.getUser().getUserId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender wallet not found"));
                walletHolder[1] = walletRepo.findByAccountNumber(request.getReceiverAccountNumber()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver wallet not found"));

            if (walletHolder[0].getBalance().compareTo(request.getAmount()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
            }
            walletHolder[0].setBalance(walletHolder[0].getBalance().subtract(request.getAmount()));
            walletHolder[1].setBalance(walletHolder[1].getBalance().add(request.getAmount()));
            walletRepo.save(walletHolder[0]);
            walletRepo.save(walletHolder[1]);
            Transaction transaction = Transaction
                    .builder()
                    .walletId(walletHolder[0])
                    .receiverWalletId(walletHolder[1])
                    .amount(request.getAmount())
                    .idempotencyKey(request.getIdempotencyKey())
                    .status(Status.SUCCESS)
                    .transactionType(TransactionType.ENTERPRISE_TRANSFER)
                    .createdAt(LocalDateTime.now())
                    .merchantType(MerchantType.ENTERPRISE)
                    .build();
            NotificationLog receiverNotificationLog = NotificationLog
                    .builder()
                    .username(walletHolder[1].getUser().getUserName())
                    .title("Successful Transfer")
                    .eventType("Success")
                    .message("You have successfully been Transferred EGP:" + transaction.getAmount() + " from " + walletHolder[0].getUser().getUserName() +".")
                    .isRead(false)
                    .timestamp(LocalDateTime.now())
                    .build();
            transactionRepo.save(transaction);
            notificationLogRepo.save(receiverNotificationLog);
            stringRedisTemplate.opsForValue().set(key, "SUCCESS", Duration.ofHours(24));
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (walletHolder[1].getUser().getStatus().equals(UserStatus.ONLINE)){
                        notificationService.sendNotification(
                                walletHolder[1].getUser().getUserName(),
                                receiverNotificationLog.getEventType(),
                                receiverNotificationLog.getTitle(),
                                receiverNotificationLog.getMessage()
                        );
                        }else {
                            notificationService.notifyUserDevice(walletHolder[1].getUser().getUserId(),transaction.getTransactionId().toString(), receiverNotificationLog.getId());
                        }
                    }
                });
            } else {
                if (walletHolder[1].getUser().getStatus().equals(UserStatus.ONLINE)) {
                    notificationService.sendNotification(walletHolder[1].getUser().getUserName(), receiverNotificationLog.getEventType(), receiverNotificationLog.getTitle(), receiverNotificationLog.getMessage());
                }else {
                    notificationService.notifyUserDevice(walletHolder[1].getUser().getUserId(),transaction.getTransactionId().toString(), receiverNotificationLog.getId());
                }
            }
            return EnterpriseTransactionResponse
                    .builder()
                    .transactionId(transaction.getTransactionId())
                    .type(transaction.getTransactionType().name())
                    .timestamp(transaction.getCreatedAt() != null ? transaction.getCreatedAt() : LocalDateTime.now())
                    .status(transaction.getStatus().name())
                    .receiverAccountNumber(transaction.getReceiverWalletId().getAccountNumber())
                    .amount(request.getAmount())
                    .idempotencyKey(request.getIdempotencyKey())
                    .build();
        } catch (Exception e) {
        stringRedisTemplate.opsForValue().set(key, "FAILED", Duration.ofHours(24));

        if (walletHolder[0] != null) {
            UUID senderId = walletHolder[0].getWalletId();
            UUID receiverId = (walletHolder[1] != null) ? walletHolder[1].getWalletId() : null;

            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transactionTemplate.execute(status -> {
                Wallet senderRef = walletRepo.findByWalletId(senderId).orElse(null);
                Wallet receiverRef = (receiverId != null) ? walletRepo.findByWalletId(receiverId).orElse(null) : null;

                return transactionRepo.save(Transaction.builder()
                        .walletId(senderRef)
                        .receiverWalletId(receiverRef)
                        .amount(request.getAmount())
                        .transactionType(TransactionType.ENTERPRISE_TRANSFER)
                        .status(Status.FAILED)
                        .idempotencyKey(request.getIdempotencyKey())
                        .createdAt(LocalDateTime.now())
                        .merchantType(MerchantType.ENTERPRISE)
                        .build());
            });
        }
        throw e;
        }
    }
    @Transactional
    public TransactionResponse getTransaction(String transactionId, Authentication auth) {
        String username = auth.getName();
        users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Wallet wallet = walletRepo.findByUser_UserName(user.getUserName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        UUID id = UUID.fromString(transactionId);
        Transaction transaction = transactionRepo.findByTransactionIdAndReceiverWalletId(id,wallet)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        TransactionResponse response = TransactionResponse
                .builder()
                .transactionId(transaction.getTransactionId())
                .type(transaction.getTransactionType().name())
                .timestamp(transaction.getCreatedAt() != null ? transaction.getCreatedAt() : LocalDateTime.now())
                .status(transaction.getStatus().name())
                .receiverUsername(user.getUserName())
                .amount(transaction.getAmount())
                .idempotencyKey(transaction.getIdempotencyKey())
                .build();
        return response;
    }
    @Transactional
    public TransactionApiResponse<List<TransactionResponse>> getTransactions(Authentication authentication) {
        String username = authentication.getName();
        users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Wallet wallet = walletRepo.findByUser_UserName(user.getUserName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        List<Transaction> transactions = transactionRepo.findByWalletId(wallet);

        List<TransactionResponse> transactionResponses = transactions.stream().map(tx -> {
            String receiverName = (tx.getReceiverWalletId() != null && tx.getReceiverWalletId().getUser() != null)
                    ? tx.getReceiverWalletId().getUser().getUserName() : null;

            return TransactionResponse.builder()
                    .transactionId(tx.getTransactionId())
                    .amount(tx.getAmount())
                    .idempotencyKey(tx.getIdempotencyKey())
                    .receiverUsername(receiverName)
                    .status(tx.getStatus().name())
                    .type(tx.getTransactionType().name())
                    .timestamp(tx.getCreatedAt() != null ? tx.getCreatedAt() : LocalDateTime.now())
                    .build();
        }).collect(Collectors.toList());

        return new TransactionApiResponse<>("ALL Transactions Retrieved", transactionResponses);
    }
    @Transactional
    public List<TransactionResponse> getEnterpriseTransactions(Authentication auth) {
        String userName = auth.getName();
        users user = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Merchant merchant = merchantRepo.findByUser_UserName(user.getUserName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found"));
        Wallet wallet = walletRepo.findByUser_UserName(user.getUserName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        List<Transaction> transactions = transactionRepo.findByWalletIdAndTransactionType(wallet,TransactionType.ENTERPRISE_TRANSFER);
        if (merchant.getEnterpriseMerchantStatus().equals(EnterpriseMerchantStatus.ACTIVE)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enterprise merchant is not active");
        }
        List<TransactionResponse> transactionResponses = transactions.stream().map(tx -> {
            String receiverName = (tx.getReceiverWalletId() != null && tx.getReceiverWalletId().getUser() != null)
                    ? tx.getReceiverWalletId().getUser().getUserName() : null;

            return TransactionResponse.builder()
                    .transactionId(tx.getTransactionId())
                    .amount(tx.getAmount())
                    .idempotencyKey(tx.getIdempotencyKey())
                    .receiverUsername(receiverName)
                    .status(tx.getStatus().name())
                    .type(tx.getTransactionType().name())
                    .timestamp(tx.getCreatedAt() != null ? tx.getCreatedAt() : LocalDateTime.now())
                    .build();
        }).collect(Collectors.toList());

        return transactionResponses;
    }
}