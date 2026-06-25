package com.jo.payonservices.onpay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ThirdPartyTransactionApiResponse<T> {
    private String message;
    private T data;
}
