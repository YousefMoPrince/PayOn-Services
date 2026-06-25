package com.jo.payonservices.auth.dto;

import lombok.Data;

@Data
public class LogoutRequest {
    private String RefreshToken;
}
