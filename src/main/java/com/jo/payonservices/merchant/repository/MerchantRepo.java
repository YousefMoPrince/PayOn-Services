package com.jo.payonservices.merchant.repository;

import com.jo.payonservices.merchant.model.EnterpriseMerchantStatus;
import com.jo.payonservices.merchant.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepo extends JpaRepository<Merchant, UUID> {
    Optional<Merchant> findByMerchantUserName(String username);
    Optional<Merchant> findByUser_UserName(String username);
    Optional<Merchant> findByAccountNumber(Long accountNumber);
    Optional<Merchant> findByApiKey(String apiKey);
    Optional<List<Merchant>> findByEnterpriseMerchantStatus(EnterpriseMerchantStatus status);
    Optional<Merchant> findByMerchantIdAndEnterpriseMerchantStatus(UUID merchantId, EnterpriseMerchantStatus status);
    @Query("SELECT m FROM Merchant m JOIN FETCH m.user WHERE m.apiKey = :apiKey")
    Optional<Merchant> findByApiKeyWithUser(@Param("apiKey") String apiKey);
}
