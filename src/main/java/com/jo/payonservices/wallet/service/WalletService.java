package com.jo.payonservices.wallet.service;

import com.jo.payonservices.auth.dto.AuthApiResponse;
import com.jo.payonservices.auth.dto.AuthResponse;
import com.jo.payonservices.auth.model.users;
import com.jo.payonservices.auth.repository.UserRepo;
import com.jo.payonservices.notifications.service.NotificationService;
import com.jo.payonservices.wallet.dto.WalletResponse;
import com.jo.payonservices.wallet.model.Wallet;
import com.jo.payonservices.wallet.model.WalletStatus;
import com.jo.payonservices.wallet.repository.WalletRepo;
import com.jo.payonservices.wallet.util.AccountUtilization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {
    @Autowired
    private WalletRepo walletRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private AccountUtilization accountUtilization;
    @Autowired
    private NotificationService notificationService;

    @Transactional
    public WalletResponse initializeWallet(Authentication auth) {
        String username = (String) auth.getPrincipal();
        users user = userRepo.findByUserName(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (walletRepo.findByUser_UserName(user.getUserName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet already exists");
        }

        Long accountNumber = accountUtilization.generateAccountNumber();
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setAccountNumber(accountNumber);
        wallet.setWalletStatus(WalletStatus.INDIVIDUAL);
        walletRepo.save(wallet);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationService.sendNotification(
                            user.getUserName(),
                            "Success",
                            "Wallet Created",
                            "Welcome to PayOn!"
                    );
                }
            });
        } else {
            notificationService.sendNotification(user.getUserName(), "Success", "Wallet Created", "Welcome to PayOn!");
        }        return new WalletResponse(wallet.getWalletId(), wallet.getUser().getUserId().toString(), wallet.getBalance(), wallet.getAccountNumber(),WalletStatus.INDIVIDUAL);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        String username = (String) auth.getPrincipal();
        users user = userRepo.findByUserName(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Wallet wallet = walletRepo.findByUser_UserName(user.getUserName()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        return new WalletResponse(wallet.getWalletId(), wallet.getUser().getUserId().toString(), wallet.getBalance(), wallet.getAccountNumber(),wallet.getWalletStatus());
    }
    public AuthApiResponse<List<AuthResponse>> syncContacts(List<String> contactPhoneNumbers) {
        List<users> matchedUsers = userRepo.findByPhoneNumberIn(contactPhoneNumbers);
        List<AuthResponse> responses = matchedUsers.stream().map(
                contacts -> AuthResponse
                        .builder()
                        .phoneNumber(contacts.getPhoneNumber())
                        .userName(contacts.getUserName())
                        .thumbnailPath(contacts.getThumbnailPath())
                        .build()
        ).toList();
        return new AuthApiResponse<>("users",responses);
    }
}