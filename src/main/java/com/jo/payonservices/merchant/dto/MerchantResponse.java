package com.jo.payonservices.merchant.dto;

import com.jo.payonservices.merchant.model.MerchantType;
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
    private MerchantType merchantType;
}
