package com.jo.payonservices.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String fullname;
    private String phonenumber;
    private String thumbnailpath;
    private LocalDateTime createdat;
}
