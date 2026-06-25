package com.jo.payonservices.wallet.repository;

import com.jo.payonservices.wallet.model.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface WalletRepo extends JpaRepository<Wallet, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(UUID userId);
    Optional<Wallet> findByUser_UserId(UUID userId);
    Optional<Wallet> findByAccountNumber(Long accountNumber);
}
