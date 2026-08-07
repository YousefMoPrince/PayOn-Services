package com.jo.payonservices.wallet.repository;

import com.jo.payonservices.wallet.model.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface WalletRepo extends JpaRepository<Wallet, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.accountNumber = :account_number")
    Optional<Wallet> findByAccountNumberForUpdate(@Param("account_number") Long accountNumber);
    Optional<Wallet> findByUser_UserName(String userName);
    Optional<Wallet> findByAccountNumber(Long accountNumber);
    Optional<Wallet> findByWalletId(UUID walletId);
}
