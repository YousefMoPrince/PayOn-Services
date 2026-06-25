package com.jo.payonservices.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantResponse {
    private UUID merchantId;
    private String merchantUserName;
    private String merchantUrl;
    private Long accountNumber;
}
