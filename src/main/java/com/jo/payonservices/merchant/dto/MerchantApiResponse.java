package com.jo.payonservices.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MerchantApiResponse<T> {
    private String message;
    private T data;

}
