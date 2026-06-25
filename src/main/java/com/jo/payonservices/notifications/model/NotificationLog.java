package com.jo.payonservices.notifications.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("NotificationLog")
public class NotificationLog {
    @Id
    private String id;
    @Indexed
    private String username;
    private String eventType;
    private String title;
    private String message;
    @Builder.Default
    private boolean isRead = false;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
