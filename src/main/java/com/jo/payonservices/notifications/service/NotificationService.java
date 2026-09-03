package com.jo.payonservices.notifications.service;

import com.jo.payonservices.auth.model.users;
import com.jo.payonservices.auth.repository.UserRepo;
import com.jo.payonservices.notifications.dto.DeviceTokenRequest;
import com.jo.payonservices.notifications.dto.NotificationResponse;
import com.jo.payonservices.notifications.model.NotificationLog;
import com.jo.payonservices.notifications.model.UserDeviceToken;
import com.jo.payonservices.notifications.repository.NotificationLogRepo;
import com.jo.payonservices.notifications.repository.UserDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationLogRepo notificationLogRepo;
    private final NotificationLogService notificationLogService;
    private final UserRepo userRepo;
    private final UserDeviceTokenRepository tokenRepository;
    private final FcmPushService fcmPushService;

    @Transactional
    public void registerDeviceToken(DeviceTokenRequest request, Authentication auth) {
        tokenRepository.deleteByDeviceToken(request.getDeviceToken());
        String username = (String) auth.getPrincipal();
        users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserDeviceToken newToken = UserDeviceToken.builder()
                .userId(user.getUserId())
                .deviceToken(request.getDeviceToken())
                .deviceType(request.getDeviceType())
                .updatedAt(LocalDateTime.now())
                .build();

        tokenRepository.save(newToken);
    }

    @Transactional
    public void deleteDeviceToken(String deviceToken, Authentication auth) {
        String username = (String) auth.getPrincipal();
        users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        tokenRepository.deleteByUserIdAndDeviceToken(user.getUserId(), deviceToken);
    }
    public void notifyUserDevice(UUID userId, String transactionId) {
        UserDeviceToken userDevices = tokenRepository.findByUserId(userId);
            fcmPushService.sendTransactionSilentSyncPush(userDevices.getDeviceToken(), transactionId);
    }
    public void notifyMerchantDevice(UUID userId, Long transactionCode) {
        UserDeviceToken userDevices = tokenRepository.findByUserId(userId);
        fcmPushService.sendReceiptSilentSyncPush(userDevices.getDeviceToken(),transactionCode);
    }
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
        log.info("Attempting to send WS notification to user: {} for event: {}", username, eventType);
        notificationLogService.saveToMongo(username, eventType, title, message);

    }
    public Page<NotificationResponse> getRecentNotifications(Authentication auth, int page, int size) {
        String username = (String) auth.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationLog> logPage = notificationLogRepo.findByUsernameOrderByTimestampDesc(username, pageable);
        return logPage.map(log -> NotificationResponse
                .builder()
                .id(log.getId())
                .eventType(log.getEventType())
                .title(log.getTitle())
                .message(log.getMessage())
                .isRead(log.isRead())
                .timestamp(log.getTimestamp())
                .build());
    }

    public long getUnReadNotifications(Authentication auth) {
        String username = (String) auth.getPrincipal();
        return notificationLogRepo.countByUsernameAndIsReadFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public void markAsRead(String notificationId, Authentication auth) {
        String userName = (String) auth.getPrincipal();
        users user = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        notificationLogRepo.findById(notificationId).ifPresent(notification -> {
            if (notification.getUsername().equals(user.getUserName())) {
                notification.setRead(true);
                notificationLogRepo.save(notification);
            }
        });
    }

    public void markAllAsRead(Authentication auth) {
        String username = (String) auth.getPrincipal();
        users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        List<NotificationLog> unreadNotifications = notificationLogRepo.findByUsernameAndIsReadFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!unreadNotifications.isEmpty()) {
            unreadNotifications.forEach(n -> n.setRead(true));
            notificationLogRepo.saveAll(unreadNotifications);
        }

    }
}
