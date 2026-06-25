package com.jo.payonservices.transactions.controller;

import com.jo.payonservices.transactions.dto.TransactionApiResponse;
import com.jo.payonservices.transactions.dto.TransactionRequest;
import com.jo.payonservices.transactions.dto.TransactionResponse;
import com.jo.payonservices.transactions.model.Status;
import com.jo.payonservices.transactions.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    @Autowired
    private TransactionService transactionService;


    @PostMapping("/deposit")
    public ResponseEntity<TransactionApiResponse<TransactionResponse>> deposit(Authentication auth,@RequestHeader(value = "Idempotency-Key", required = false) String idempotency,@RequestBody TransactionRequest request) {
        if (idempotency == null || idempotency.isBlank()) {
            idempotency = java.util.UUID.randomUUID().toString();
        }

        request.setIdempotencyKey(idempotency);
        TransactionResponse response = transactionService.deposit(auth, request);
        TransactionApiResponse<TransactionResponse> body = new TransactionApiResponse<>("Deposit Operation", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }


    @PostMapping("/withdraw")
    public ResponseEntity<TransactionApiResponse<TransactionResponse>> withdraw(Authentication auth,@RequestHeader(value = "Idempotency-Key", required = false) String idempotency, @RequestBody TransactionRequest request) {
        if (idempotency == null || idempotency.isBlank()) {
            idempotency = java.util.UUID.randomUUID().toString();
        }

        request.setIdempotencyKey(idempotency);
        TransactionResponse response = transactionService.withdraw(auth, request);
        TransactionApiResponse<TransactionResponse> body = new TransactionApiResponse<>("Withdraw Operation", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }


    @PostMapping("/transfer")
    public ResponseEntity<TransactionApiResponse<TransactionResponse>> transfer(Authentication auth, @RequestHeader(value = "Idempotency-Key", required = false) String idempotency, @RequestBody TransactionRequest request) {
        if (idempotency == null || idempotency.isBlank()) {
            idempotency = java.util.UUID.randomUUID().toString();
        }

        request.setIdempotencyKey(idempotency);
        TransactionResponse response = transactionService.transfer(auth, request);
        TransactionApiResponse<TransactionResponse> body = new TransactionApiResponse<>("Transfer Operation", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/transfer/ack")
    public ResponseEntity<TransactionApiResponse<TransactionResponse>> transferAck(
            @RequestParam String idempotencyKey,
            @RequestParam Status state) {
        TransactionResponse response = transactionService.transferAck(idempotencyKey, state);
        TransactionApiResponse<TransactionResponse> body = new TransactionApiResponse<>("Transfer Acknowledgment Operation", response);
        return ResponseEntity.ok(body);
    }


    @GetMapping("/getTransactions")
    public ResponseEntity<TransactionApiResponse<List<TransactionResponse>>> getTransactions(Authentication auth) {
        TransactionApiResponse<List<TransactionResponse>> response = transactionService.getTransactions(auth);
        return ResponseEntity.ok(response);
    }
}
