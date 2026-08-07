package com.jo.payonservices.merchant.service;

import com.jo.payonservices.auth.model.users;
import com.jo.payonservices.auth.repository.UserRepo;
import com.jo.payonservices.merchant.dto.BecomeMerchantRequest;
import com.jo.payonservices.merchant.dto.MerchantResponse;
import com.jo.payonservices.merchant.model.Merchant;
import com.jo.payonservices.merchant.model.MerchantType;
import com.jo.payonservices.merchant.repository.MerchantRepo;
import com.jo.payonservices.notifications.service.NotificationService;
import com.jo.payonservices.wallet.model.Wallet;
import com.jo.payonservices.wallet.repository.WalletRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {
    @Autowired
    private MerchantRepo merchantRepo;
    @Autowired
    private WalletRepo walletRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private NotificationService notificationService;

    @Transactional
    public MerchantResponse becomeMerchant(Authentication auth, BecomeMerchantRequest request) {
        String username = (String) auth.getPrincipal();
        users user = userRepo.findByUserName(username).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Wallet wallet = walletRepo.findByUser_UserName(user.getUserName()).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        if (merchantRepo.findByAccountNumber(wallet.getAccountNumber()).isPresent()) {
            log.error("Registration failed: Account number {} is already registered as a merchant", wallet.getAccountNumber());
            throw new IllegalStateException("This account number is already registered under another merchant profile!");
        }
         if (merchantRepo.findByMerchantUserName(request.getMerchantUserName()).isPresent()) {
          log.error("Registration failed: UserName {} is already registered as a merchant", request.getMerchantUserName());
          throw new IllegalStateException("This userName is already registered under another merchant profile!");
         }
         Merchant merchant = new Merchant();
         merchant.setMerchantUserName(request.getMerchantUserName());
         merchant.setUser(user);
         merchant.setAccountNumber(wallet.getAccountNumber());
         merchant.setMerchantType(MerchantType.INDIVIDUAL);
         merchantRepo.save(merchant);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationService.sendNotification(
                            user.getUserName(),
                            "Success",
                            "Merchant",
                            "Welcome to Merchant Dashboard!"
                    );
                }
            });
        } else {
            notificationService.sendNotification(user.getUserName(), "Success", "Merchant", "Welcome to Merchant Dashboard!");
        }
        return new MerchantResponse(merchant.getMerchantId(),merchant.getMerchantUserName(),merchant.getMerchantUrl(),merchant.getAccountNumber(),merchant.getMerchantType());
    }

    @Transactional(readOnly = true)
    public MerchantResponse getMerchant(Authentication auth) {
        String username = (String) auth.getPrincipal();
        users user = userRepo.findByUserName(username).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Wallet wallet = walletRepo.findByUser_UserName(user.getUserName()).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        Merchant merchant = merchantRepo.findByAccountNumber(wallet.getAccountNumber()).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found"));
        return new MerchantResponse(merchant.getMerchantId(),merchant.getMerchantUserName(),null,wallet.getAccountNumber(),merchant.getMerchantType());
    }
}
