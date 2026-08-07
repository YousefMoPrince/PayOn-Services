package com.jo.payonservices.notifications.controller;

import com.jo.payonservices.notifications.dto.NotificationResponse;
import com.jo.payonservices.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    @GetMapping("/recent")
    public ResponseEntity<Page<NotificationResponse>> getRecentNotifications(Authentication auth, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "15") int size) {
        Page<NotificationResponse> notifications = notificationService.getRecentNotifications(auth, page, size);
        return ResponseEntity.ok(notifications);
    }
    @GetMapping("/recent/unread")
    public ResponseEntity<Long> getUnReadNotifications(Authentication auth){
        return ResponseEntity.ok().body(notificationService.getUnReadNotifications(auth));
    }
    @PatchMapping("/read/{notificationId}")
    public ResponseEntity<Void> readNotification(@PathVariable String notificationId, Authentication auth) {
        notificationService.markAsRead(notificationId, auth);
        return ResponseEntity.ok().build();
    }
    @PatchMapping("/read/all")
    public ResponseEntity<Void> readAllNotifications(Authentication auth) {
        notificationService.markAllAsRead(auth);
        return ResponseEntity.ok().build();
    }

}
