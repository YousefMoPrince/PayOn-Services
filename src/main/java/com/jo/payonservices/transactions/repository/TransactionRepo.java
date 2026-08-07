package com.jo.payonservices.transactions.repository;

import com.jo.payonservices.transactions.model.Transaction;
import com.jo.payonservices.transactions.model.TransactionType;
import com.jo.payonservices.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    List<Transaction> findByWalletId(Wallet wallet);
    List<Transaction> findByWalletIdAndTransactionType(Wallet wallet, TransactionType transactionType);
}
