package com.jo.payonservices.auth.dto;

import lombok.Data;


import java.time.LocalDateTime;

@Data
public class UpdateRequest {
    private String username;
    private String email;
    private String password;
    private String fullname;
    private String phonenumber;
}
