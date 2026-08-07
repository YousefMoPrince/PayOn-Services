package com.jo.payonservices.merchant.dto;

import com.jo.payonservices.auth.model.Role;
import com.jo.payonservices.merchant.model.EnterpriseMerchantStatus;
import com.jo.payonservices.merchant.model.MerchantType;
import com.jo.payonservices.onpay.dto.ThirdPartyTransactionResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class EnterpriseMerchantResponse {
    private Role companyRole;
    private EnterpriseMerchantDataResponse data;
    @Data
    @Builder
    public static class EnterpriseMerchantDataResponse {
        private String companyName;
        private String companyEmail;
        private String companyDial;
        private String companyPassword;
        private MerchantType companyType;
        private EnterpriseMerchantStatus companyStatus;
        private String companyTaxId;
        private Long companyAccountNumber;
        private BigDecimal companyBalance;
        private String companyApiKey;
        private String companyApiSecret;
        private String companyUrl;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
