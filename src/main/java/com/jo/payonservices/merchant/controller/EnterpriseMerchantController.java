package com.jo.payonservices.merchant.controller;

import com.jo.payonservices.merchant.service.EnterpriseMerchantService;
import com.jo.payonservices.onpay.dto.ThirdPartyTransactionApiResponse;
import com.jo.payonservices.onpay.dto.ThirdPartyTransactionRequest;
import com.jo.payonservices.onpay.dto.ThirdPartyTransactionResponse;
import com.jo.payonservices.onpay.service.ThirdPartyTransactionService;
import com.jo.payonservices.transactions.dto.*;
import com.jo.payonservices.transactions.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/enterprise/merchant")
public class EnterpriseMerchantController {
    @Autowired
    private EnterpriseMerchantService enterpriseMerchantService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private ThirdPartyTransactionService thirdPartyTransactionService;

    @PostMapping("/rotate")
    public ResponseEntity<Map<String, String>> rejectMerchant(Authentication auth) {
        return ResponseEntity.ok().body(enterpriseMerchantService.rotateApiKeys(auth));
    }
    @PostMapping("/transfer")
    public ResponseEntity<EnterpriseTransactionResponse> enterpriseTransfer(Authentication auth, @RequestBody EnterpriseTransactionRequest request) {
        EnterpriseTransactionResponse response = transactionService.enterpriseTransfer(auth, request);
        return ResponseEntity.status(HttpStatus.CREATED).body( response);
    }
    @GetMapping("/transactions")
    public ResponseEntity<TransactionApiResponse<List<TransactionResponse>>> getAllTransactions(Authentication auth) {
        List<TransactionResponse> responses = transactionService.getEnterpriseTransactions(auth);
       return ResponseEntity.ok().body(new TransactionApiResponse<>("All enterprise transactions fetched",responses));
    }
    @PostMapping("/receipts/create")
    public ResponseEntity<ThirdPartyTransactionResponse> createReceipt(Authentication auth, @RequestBody ThirdPartyTransactionRequest request) {
        ThirdPartyTransactionResponse response = thirdPartyTransactionService.createEnterpriseReceipt(auth, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/receipts")
    public ResponseEntity<ThirdPartyTransactionApiResponse<List<ThirdPartyTransactionResponse>>> getAllReceipts(Authentication auth) {
        List<ThirdPartyTransactionResponse> responses = thirdPartyTransactionService.getEnterpriseReceipts(auth);
        return ResponseEntity.ok(new ThirdPartyTransactionApiResponse<>("All enterprise receipts fetched",responses));
    }

}
