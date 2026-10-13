package com.jo.payonservices.merchant.controller;

import com.jo.payonservices.merchant.dto.BecomeMerchantRequest;
import com.jo.payonservices.merchant.dto.MerchantApiResponse;
import com.jo.payonservices.merchant.dto.MerchantResponse;
import com.jo.payonservices.merchant.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/merchant")
public class MerchantController {
    @Autowired
    private MerchantService merchantService;

    @PostMapping("/becomeMerchant")
    public ResponseEntity<MerchantApiResponse<MerchantResponse>> becomeMerchant(Authentication auth,@RequestBody BecomeMerchantRequest request) {
        MerchantResponse response = merchantService.becomeMerchant(auth, request);
        return new ResponseEntity<>(new MerchantApiResponse<>("Becoming Merchant Succeeded",response), HttpStatus.CREATED);
    }
    @GetMapping("/getMerchant")
    public ResponseEntity<MerchantApiResponse<MerchantResponse>> getMerchant(Authentication authentication) {
        MerchantResponse response = merchantService.getMerchant(authentication);
        return new ResponseEntity<>(new MerchantApiResponse<>("Merchant Retrieved",response), HttpStatus.OK);
    }
}
