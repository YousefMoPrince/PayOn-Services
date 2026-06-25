package com.jo.payonservices.wallet.controller;

import com.jo.payonservices.auth.dto.AuthApiResponse;
import com.jo.payonservices.auth.dto.AuthResponse;
import com.jo.payonservices.wallet.dto.WalletApiResponse;
import com.jo.payonservices.wallet.dto.WalletResponse;
import com.jo.payonservices.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {
    @Autowired
    private WalletService walletService;

    @PostMapping("/initial")
    public ResponseEntity<WalletApiResponse<WalletResponse>> initialWallet(Authentication authentication) {
        WalletResponse response = walletService.initializeWallet(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(new WalletApiResponse<>("Wallet initialized", response));
    }
    @PostMapping("/syncContacts")
    public ResponseEntity<AuthApiResponse<List<AuthResponse>>> syncContacts(@RequestBody List<String> contactPhoneNumbers) {
        AuthApiResponse<List<AuthResponse>> response = walletService.syncContacts(contactPhoneNumbers);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/walletInfo")
    public ResponseEntity<WalletApiResponse<WalletResponse>> walletInfo(Authentication authentication) {
        WalletResponse response = walletService.getWallet(authentication);
        return ResponseEntity.status(HttpStatus.OK).body(new WalletApiResponse<>("Wallet info", response));
    }


}
