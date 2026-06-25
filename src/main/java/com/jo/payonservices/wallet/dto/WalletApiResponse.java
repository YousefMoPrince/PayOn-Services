package com.jo.payonservices.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class WalletApiResponse<T>{
    private String message;
    private T data;
}
