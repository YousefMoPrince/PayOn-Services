package com.jo.payonservices.notifications.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private String id;
    private String eventType;
    private String title;
    private String message;
    @JsonProperty("isRead")
    private boolean isRead;
    private LocalDateTime timestamp;
}
