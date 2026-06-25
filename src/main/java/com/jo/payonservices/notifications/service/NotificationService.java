package com.jo.payonservices.notifications.service;

import com.jo.payonservices.notifications.dto.NotificationResponse;
import com.jo.payonservices.notifications.model.NotificationLog;
import com.jo.payonservices.notifications.repository.NotificationLogRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationLogRepo notificationLogRepo;
    private final NotificationLogService notificationLogService;

    public void sendNotification(String username, String eventType, String title, String message) {
        NotificationResponse notificationResponse = NotificationResponse
                .builder()
                .eventType(eventType)
                .title(title)
                .message(message)
                .isRead(false)
                .timestamp(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                notificationResponse
        );
        notificationLogService.saveToMongo(username, eventType, title, message);

    }
    public Page<NotificationResponse> getRecentNotifications(Authentication auth, int page, int size) {
        String username = (String) auth.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationLog> logPage = notificationLogRepo.findByUsernameOrderByTimestampDesc(username,pageable);
        return logPage.map(log->NotificationResponse
                .builder()
                .id(log.getId())
                .eventType(log.getEventType())
                .title(log.getTitle())
                .message(log.getMessage())
                .isRead(log.isRead())
                .timestamp(log.getTimestamp())
                .build());
    }

}
