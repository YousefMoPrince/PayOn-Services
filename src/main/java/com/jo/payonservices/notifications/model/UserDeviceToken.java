package com.jo.payonservices.notifications.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "user_device_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDeviceToken {
    @Id
    private String id;

    @Indexed
    private UUID userId;

    @Indexed(unique = true)
    private String deviceToken;

    private String deviceType;
    private LocalDateTime updatedAt;
}