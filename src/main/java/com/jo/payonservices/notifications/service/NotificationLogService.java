package com.jo.payonservices.notifications.service;

import com.jo.payonservices.notifications.model.NotificationLog;
import com.jo.payonservices.notifications.repository.NotificationLogRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationLogService {
    private final NotificationLogRepo notificationLogRepo;
    @Async
    protected void saveToMongo(String username, String eventType, String title, String message) {
        try {
            NotificationLog log = NotificationLog
                    .builder()
                    .username(username)
                    .eventType(eventType)
                    .title(title)
                    .message(message)
                    .isRead(true)
                    .timestamp(LocalDateTime.now())
                    .build();
            notificationLogRepo.save(log);

        }catch (Exception e) {
            log.error("Failed to async save notification log to MongoDB for user: {}", username, e);
            throw new RuntimeException(e);
        }

    }
}
