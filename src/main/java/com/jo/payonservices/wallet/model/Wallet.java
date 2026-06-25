package com.jo.payonservices.wallet.model;

import com.jo.payonservices.auth.model.users;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Table(name = "wallets")
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_id")
    private UUID walletId;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,referencedColumnName = "user_id")
    private users user;
    @Column(name = "balance")
    private BigDecimal balance;
    @Column(name = "account_number",nullable = false,length = 50)
    private Long accountNumber;

}
