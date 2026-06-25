package com.jo.payonservices.transactions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;


@Data
@AllArgsConstructor
public class TransactionApiResponse<T>{
    private String message;
    private T data;
}
