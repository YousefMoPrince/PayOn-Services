package com.jo.payonservices.onpay.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ThirdPartyTransactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnPay {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique = true, nullable = false,updatable = false,name = "third_party_transaction_id")
    private UUID thirdPartyTransactionId;
    @Column(name = "merchant_username")
    private String merchantUserName;
    @Column(name = "account_number",nullable = false)
    private Long accountNumber;
    @Column(name = "payer_account_number")
    private Long payerAccountNumber;
    @Column(name = "transaction_code",nullable = false)
    private Long transactionCode;
    @Column(name = "amount",nullable = false)
    private BigDecimal amount;
    @Column(name = "currency",nullable = false,updatable = false,length = 3)
    private String currency;
    @Column(name = "status",nullable = false)
    private String status;
    @Column(name = "reference_key",nullable = false,updatable = false)
    private String referenceKey;
    @Column(name = "idempotency_key",nullable = false,updatable = false)
    private UUID idempotencyKey;
    @Column(name = "created_at",nullable = false,updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
