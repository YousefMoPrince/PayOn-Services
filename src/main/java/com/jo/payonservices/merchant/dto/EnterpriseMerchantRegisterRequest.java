package com.jo.payonservices.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnterpriseMerchantRegisterRequest {
    private String companyName;
    private String email;
    private String taxId;
    private String phoneNumber;
    private String webhookUrl;
}