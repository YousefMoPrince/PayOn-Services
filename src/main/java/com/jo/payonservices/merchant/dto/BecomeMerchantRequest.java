package com.jo.payonservices.merchant.dto;

import lombok.Data;

@Data
public class BecomeMerchantRequest {
    private String merchantUserName;
    private String merchantUrl;
    private Long accountNumber;
}
