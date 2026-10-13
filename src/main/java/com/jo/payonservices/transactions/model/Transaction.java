package com.jo.payonservices.transactions.model;

import com.jo.payonservices.merchant.model.MerchantType;
import com.jo.payonservices.wallet.model.Wallet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id",nullable = false)
    private Wallet walletId;
    @ManyToOne
    @JoinColumn(name = "receiver_wallet_id")
    private Wallet receiverWalletId;
    @Column(name = "amount",nullable = false,precision = 15,scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;
    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false)
    private Status status;
    @Enumerated(EnumType.STRING)
    @Column(name = "merchant_type")
    private MerchantType merchantType;
    @Column(name = "idempotency_key",nullable = false,length = 100)
    private String idempotencyKey;
    @CreationTimestamp
    @Column(name = "created_at",nullable = false,updatable = false)
    private LocalDateTime createdAt;


}
