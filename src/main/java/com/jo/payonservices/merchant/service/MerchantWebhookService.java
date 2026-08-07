package com.jo.payonservices.merchant.service;

import com.jo.payonservices.merchant.dto.EnterpriseMerchantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MerchantWebhookService {
    private final RestClient restClient = RestClient.create();
    @Async
    public void sendApprovalWebhook(EnterpriseMerchantResponse response) {
        String url = response.getData().getCompanyUrl()+"api/web/v1/payment/webhook/receive";

        EnterpriseMerchantResponse.EnterpriseMerchantDataResponse merchantData = response.getData();

        Map<String, Object> payload = Map.of(
                "companyRole", response.getCompanyRole(),
                "data", Map.ofEntries(
                        Map.entry("companyName", merchantData.getCompanyName()),
                        Map.entry("companyEmail", merchantData.getCompanyEmail()),
                        Map.entry("companyDial", merchantData.getCompanyDial()),
                        Map.entry("companyPassword", merchantData.getCompanyPassword()),
                        Map.entry("companyType", merchantData.getCompanyType()),
                        Map.entry("companyStatus", merchantData.getCompanyStatus()),
                        Map.entry("companyTaxId", merchantData.getCompanyTaxId()),
                        Map.entry("companyAccountNumber", merchantData.getCompanyAccountNumber()),
                        Map.entry("companyBalance", merchantData.getCompanyBalance()),
                        Map.entry("companyApiKey", merchantData.getCompanyApiKey()),
                        Map.entry("companyApiSecret", merchantData.getCompanyApiSecret()),
                        Map.entry("companyUrl", merchantData.getCompanyUrl()),
                        Map.entry("createdAt", merchantData.getCreatedAt()),
                        Map.entry("updatedAt", merchantData.getUpdatedAt())
                )
        );
        try {
        restClient.post()
                .uri(url)
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }    @Async
    public void sendRejectionWebhook(EnterpriseMerchantResponse response){
        String url = response.getData().getCompanyUrl()+"api/web/v1/payment/webhook/receive";

        EnterpriseMerchantResponse.EnterpriseMerchantDataResponse merchantData = response.getData();

        Map<String, Object> payload = Map.of(
                "companyRole", response.getCompanyRole(),
                "data", Map.ofEntries(
                        Map.entry("companyStatus", merchantData.getCompanyStatus())

                )
        );
        try {
        restClient.post()
                .uri(url)
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
