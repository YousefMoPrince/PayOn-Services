package com.jo.payonservices.merchant.repository;

import com.jo.payonservices.merchant.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepo extends JpaRepository<Merchant, UUID> {
    Optional<Merchant> findByMerchantUserName(String username);
    Optional<Merchant> findByAccountNumber(Long accountNumber);

}
