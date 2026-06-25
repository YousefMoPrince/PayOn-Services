package com.jo.payonservices.unit.transactions;

import com.jo.payonservices.auth.model.users;
import com.jo.payonservices.auth.repository.UserRepo;
import com.jo.payonservices.transactions.dto.TransactionApiResponse;
import com.jo.payonservices.transactions.dto.TransactionRequest;
import com.jo.payonservices.transactions.dto.TransactionResponse;
import com.jo.payonservices.transactions.model.Status;
import com.jo.payonservices.transactions.model.Transaction;
import com.jo.payonservices.transactions.model.TransactionType;
import com.jo.payonservices.transactions.repository.TransactionRepo;
import com.jo.payonservices.transactions.service.TransactionService;
import com.jo.payonservices.wallet.model.Wallet;
import com.jo.payonservices.wallet.repository.WalletRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepo transactionRepo;
    @Mock private WalletRepo walletRepo;
    @Mock private UserRepo userRepo;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private Authentication authentication;

    @InjectMocks
    private TransactionService transactionService;

    private users mockUser;
    private Wallet mockWallet;
    private UUID mockUserId;
    private UUID mockWalletId;

    @BeforeEach
    void setUp() {
        mockUserId = UUID.randomUUID();
        mockWalletId = UUID.randomUUID();

        mockUser = users.builder().userId(mockUserId).userName("yousef").build();
        mockWallet = Wallet.builder().walletId(mockWalletId).user(mockUser).balance(new BigDecimal("100.00")).build();

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // محاكاة الـ TransactionTemplate لتنفيذ الكول باك مباشرة لتفادي مشاكل الـ Scope
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    // ==========================================
    // DEPOSIT TESTS
    // ==========================================

    @Test
    @DisplayName("Deposit - Success Scenario")
    void deposit_Success() {
        TransactionRequest request = mock(TransactionRequest.class);
        when(request.getAmount()).thenReturn(new BigDecimal("50.00"));
        when(request.getIdempotencyKey()).thenReturn("tx-dep-123");

        when(authentication.getPrincipal()).thenReturn("yousef");
        when(valueOperations.setIfAbsent(eq("tx:idempotency:tx-dep-123"), eq("Processing"), any(Duration.class))).thenReturn(true);
        when(userRepo.findByUserName("yousef")).thenReturn(Optional.of(mockUser));
        when(walletRepo.findByUserIdForUpdate(mockUserId)).thenReturn(Optional.of(mockWallet));

        TransactionResponse response = transactionService.deposit(authentication, request);

        assertNotNull(response);
        assertEquals(Status.SUCCESS.name(), response.getStatus());
        assertEquals(new BigDecimal("150.00"), mockWallet.getBalance());

        verify(walletRepo).save(mockWallet);
        verify(transactionRepo).save(argThat(tx -> tx.getStatus() == Status.SUCCESS && tx.getTransactionType() == TransactionType.DEPOSIT));
        verify(valueOperations).set(eq("tx:idempotency:tx-dep-123"), eq("SUCCESS"), any(Duration.class));
    }

    // ==========================================
    // WITHDRAW TESTS
    // ==========================================

    @Test
    @DisplayName("Withdraw - Success Scenario")
    void withdraw_Success() {
        TransactionRequest request = mock(TransactionRequest.class);
        when(request.getAmount()).thenReturn(new BigDecimal("40.00"));
        when(request.getIdempotencyKey()).thenReturn("tx-with-999");

        when(authentication.getPrincipal()).thenReturn("yousef");
        when(valueOperations.setIfAbsent(eq("tx:idempotency:tx-with-999"), eq("Processing"), any(Duration.class))).thenReturn(true);
        when(userRepo.findByUserName("yousef")).thenReturn(Optional.of(mockUser));
        when(walletRepo.findByUserIdForUpdate(mockUserId)).thenReturn(Optional.of(mockWallet));

        TransactionResponse response = transactionService.withdraw(authentication, request);

        assertNotNull(response);
        assertEquals(Status.SUCCESS.name(), response.getStatus());
        assertEquals(new BigDecimal("60.00"), mockWallet.getBalance());

        verify(walletRepo).save(mockWallet);
        verify(transactionRepo).save(argThat(tx -> tx.getStatus() == Status.SUCCESS && tx.getTransactionType() == TransactionType.WITHDRAWAL));
        verify(valueOperations).set(eq("tx:idempotency:tx-with-999"), eq("SUCCESS"), any(Duration.class));
    }

    @Test
    @DisplayName("Withdraw - Insufficient Balance Saves FAILED Transaction")
    void withdraw_InsufficientBalance_ThrowsExceptionAndSavesFailed() {
        TransactionRequest request = mock(TransactionRequest.class);
        when(request.getAmount()).thenReturn(new BigDecimal("500.00")); // أكبر من الرصيد 100
        when(request.getIdempotencyKey()).thenReturn("tx-with-fail");

        when(authentication.getPrincipal()).thenReturn("yousef");
        when(valueOperations.setIfAbsent(eq("tx:idempotency:tx-with-fail"), eq("Processing"), any(Duration.class))).thenReturn(true);
        when(userRepo.findByUserName("yousef")).thenReturn(Optional.of(mockUser));
        when(walletRepo.findByUserIdForUpdate(mockUserId)).thenReturn(Optional.of(mockWallet));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                transactionService.withdraw(authentication, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Insufficient balance", exception.getReason());

        // التعديل الأهم: التحقق من أن السيرفيس بتسجل العملية على إنها FAILED في الداتا بيز وراديس
        verify(valueOperations).set(eq("tx:idempotency:tx-with-fail"), eq("FAILED"), any(Duration.class));
        verify(transactionRepo).save(argThat(tx -> tx.getStatus() == Status.FAILED && tx.getTransactionType() == TransactionType.WITHDRAWAL));
    }

    // ==========================================
    // TRANSFER TESTS
    // ==========================================

    @Test
    @DisplayName("Transfer - Successfully Initiates Pending Status")
    void transfer_Initiation_Success() {
        TransactionRequest request = mock(TransactionRequest.class);
        when(request.getAmount()).thenReturn(new BigDecimal("40.00"));
        when(request.getReceiverUsername()).thenReturn("omar");
        when(request.getIdempotencyKey()).thenReturn("tx-trn-01");

        UUID receiverId = UUID.randomUUID();
        users receiver = users.builder().userId(receiverId).userName("omar").build();
        Wallet receiverWallet = Wallet.builder().walletId(UUID.randomUUID()).user(receiver).balance(BigDecimal.ZERO).build();

        when(authentication.getPrincipal()).thenReturn("yousef");
        when(valueOperations.setIfAbsent(eq("tx:idempotency:tx-trn-01"), eq("Processing"), any(Duration.class))).thenReturn(true);
        when(userRepo.findByUserName("yousef")).thenReturn(Optional.of(mockUser));
        when(userRepo.findByUserName("omar")).thenReturn(Optional.of(receiver));

        when(walletRepo.findByUserIdForUpdate(mockUserId)).thenReturn(Optional.of(mockWallet));
        when(walletRepo.findByUserIdForUpdate(receiverId)).thenReturn(Optional.of(receiverWallet));

        TransactionResponse response = transactionService.transfer(authentication, request);

        assertNotNull(response);
        assertEquals(Status.PENDING.name(), response.getStatus());
        assertEquals("omar", response.getReceiverUsername());

        verify(transactionRepo).save(argThat(tx -> tx.getStatus() == Status.PENDING));
        verify(valueOperations).set(eq("tx:idempotency:tx-trn-01"), eq("PENDING"), any(Duration.class));
    }

    // ==========================================
    // TRANSFER ACK TESTS
    // ==========================================

    @Test
    @DisplayName("TransferAck - Finalizes Transaction and Updates Balances")
    void transferAck_Commit_Success() {
        UUID txId = UUID.randomUUID();
        UUID receiverUserId = UUID.randomUUID();
        users receiver = users.builder().userId(receiverUserId).userName("omar").build();
        Wallet mockReceiverWallet = Wallet.builder().walletId(UUID.randomUUID()).user(receiver).balance(BigDecimal.ZERO).build();

        Transaction pendingTx = Transaction.builder()
                .transactionid(txId)
                .walletId(mockWallet)
                .receiverWalletId(mockReceiverWallet)
                .amount(new BigDecimal("30.00"))
                .transactionType(TransactionType.TRANSFER)
                .status(Status.PENDING)
                .idempotencyKey("tx-trn-01")
                .build();

        when(transactionRepo.findByIdempotencyKey("tx-trn-01")).thenReturn(Optional.of(pendingTx));
        when(valueOperations.setIfAbsent(eq("tx:ack:idempotency:tx-trn-01"), eq("Processing"), any(Duration.class))).thenReturn(true);
        when(walletRepo.findByUserIdForUpdate(mockUserId)).thenReturn(Optional.of(mockWallet));
        when(walletRepo.findByUserIdForUpdate(receiverUserId)).thenReturn(Optional.of(mockReceiverWallet));

        TransactionResponse response = transactionService.transferAck("tx-trn-01", Status.SUCCESS);

        assertNotNull(response);
        assertEquals(Status.SUCCESS.name(), response.getStatus());
        assertEquals(new BigDecimal("70.00"), mockWallet.getBalance());
        assertEquals(new BigDecimal("30.00"), mockReceiverWallet.getBalance());

        verify(walletRepo).save(mockWallet);
        verify(walletRepo).save(mockReceiverWallet);
        verify(transactionRepo).save(pendingTx);

        // التحقق من تحديث المفتاحين في راديس
        verify(valueOperations).set(eq("tx:idempotency:tx-trn-01"), eq("SUCCESS"), any(Duration.class));
        verify(valueOperations).set(eq("tx:ack:idempotency:tx-trn-01"), eq("SUCCESS"), any(Duration.class));
    }

    // ==========================================
    // GET TRANSACTIONS TESTS
    // ==========================================

    @Test
    @DisplayName("GetTransactions - Successfully Retrieves All Records")
    void getTransactions_Success() {
        when(authentication.getPrincipal()).thenReturn("yousef");
        when(userRepo.findByUserName("yousef")).thenReturn(Optional.of(mockUser));
        when(walletRepo.findByUser_UserId(mockUserId)).thenReturn(Optional.of(mockWallet));

        Transaction mockTx = Transaction.builder()
                .transactionid(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .transactionType(TransactionType.DEPOSIT)
                .status(Status.SUCCESS)
                .idempotencyKey("tx-123")
                .build();

        when(transactionRepo.findByWalletId(mockWallet)).thenReturn(List.of(mockTx));

        TransactionApiResponse<List<TransactionResponse>> apiResponse = transactionService.getTransactions(authentication);

        assertNotNull(apiResponse);
        assertEquals("ALL Transactions Retrieved", apiResponse.getMessage());
        assertEquals(1, apiResponse.getData().size());
        assertEquals("tx-123", apiResponse.getData().get(0).getIdempotencyKey());
    }
}