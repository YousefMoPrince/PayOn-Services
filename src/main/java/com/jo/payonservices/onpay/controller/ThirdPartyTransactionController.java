package com.jo.payonservices.onpay.controller;

import com.jo.payonservices.onpay.dto.ReceiptResponse;
import com.jo.payonservices.onpay.dto.ThirdPartyTransactionApiResponse;
import com.jo.payonservices.onpay.dto.ThirdPartyTransactionRequest;
import com.jo.payonservices.onpay.dto.ThirdPartyTransactionResponse;
import com.jo.payonservices.onpay.service.ThirdPartyTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onPay")
@RequiredArgsConstructor
public class ThirdPartyTransactionController {

    private final ThirdPartyTransactionService thirdPartyTransactionService;

    @PostMapping("/receipts/webhook")
    public ResponseEntity<ThirdPartyTransactionApiResponse<ThirdPartyTransactionResponse>> createReceipt(
            @RequestBody ThirdPartyTransactionRequest request) {

        ThirdPartyTransactionResponse response = thirdPartyTransactionService.newReceipt(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ThirdPartyTransactionApiResponse<>("Receipt created successfully", response));
    }

    @GetMapping("/receipts/{transactionCode}")
    public ResponseEntity<ThirdPartyTransactionApiResponse<ReceiptResponse>> getReceipt(
            @PathVariable Long transactionCode) {

        ReceiptResponse response = thirdPartyTransactionService.getReceipt(transactionCode);
        return ResponseEntity.ok(
                new ThirdPartyTransactionApiResponse<>("Receipt retrieved successfully", response));
    }

    @PostMapping("/receipts/{transactionCode}/pay")
    public ResponseEntity<ThirdPartyTransactionApiResponse<String>> processPayment(
            Authentication auth,
            @PathVariable Long transactionCode) {
        thirdPartyTransactionService.processReceipt(auth, transactionCode);
        return ResponseEntity.ok(
                new ThirdPartyTransactionApiResponse<>("Payment processed successfully", "SUCCESS"));
    }

    @GetMapping("/receipts")
    public ResponseEntity<ThirdPartyTransactionApiResponse<List<ReceiptResponse>>> getAllReceipts(
            Authentication auth) {
        ThirdPartyTransactionApiResponse<List<ReceiptResponse>> response = thirdPartyTransactionService.getReceipts(auth);
        return ResponseEntity.ok(response);
    }
}