package com.jo.payonservices.onpay.repository;

import com.jo.payonservices.merchant.model.MerchantType;
import com.jo.payonservices.onpay.model.OnPay;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OnPayRepo extends JpaRepository<OnPay, UUID> {
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT o FROM OnPay o WHERE o.transactionCode = :code")
Optional<OnPay> findByTransactionCodeForUpdate(@Param("code") Long code);
Optional<OnPay> findByTransactionCode(Long transactionCode);
Optional<OnPay> findByReferenceKey(String referenceKey);
@Query("SELECT o FROM OnPay o WHERE o.accountNumber = :accountNumber OR o.payerAccountNumber = :accountNumber")
List<OnPay> findAllUserTransactions(@Param("accountNumber") Long accountNumber);
List<OnPay> findAllUserTransactionsByAccountNumberAndMerchantType(Long accountNumber, MerchantType merchantType);
Optional<List<OnPay>> findByMerchantUserName(String merchantUserName);
Optional<OnPay> findByIdempotencyKey(UUID idempotencyKey);
Optional<OnPay> findByThirdPartyTransactionIdAndAccountNumber(UUID thirdPartyTransactionId, Long accountNumber);
}
