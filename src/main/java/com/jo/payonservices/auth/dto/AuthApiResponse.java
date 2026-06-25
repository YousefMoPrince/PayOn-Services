package com.jo.payonservices.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AuthApiResponse<T> {
    private String message;
    private T data;
}
