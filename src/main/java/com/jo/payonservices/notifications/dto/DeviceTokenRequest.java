package com.jo.payonservices.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceTokenRequest {
    @NotBlank(message = "Device token is required")
    private String deviceToken;
    private String deviceType;
}