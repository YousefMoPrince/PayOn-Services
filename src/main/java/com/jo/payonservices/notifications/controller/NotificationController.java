package com.jo.payonservices.notifications.controller;

import com.jo.payonservices.notifications.dto.NotificationResponse;
import com.jo.payonservices.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


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
}
