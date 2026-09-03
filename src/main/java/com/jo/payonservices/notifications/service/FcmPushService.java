package com.jo.payonservices.notifications.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcmPushService {

    public void sendTransactionSilentSyncPush(String deviceToken, String transactionId) {
        try {
            Message message = Message.builder()
                    .setToken(deviceToken)
                    .putData("transaction_id", transactionId)
                    .putData("type", "SILENT_SYNC")
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("FCM Silent Push dispatched successfully. Message ID: {}, Transaction ID: {}", messageId, transactionId);
        } catch (Exception e) {
            log.error("Failed to send FCM Silent Push for Transaction ID {}: {}", transactionId, e.getMessage(), e);
        }
    }
    public void sendReceiptSilentSyncPush(String deviceToken, Long transactionCode) {
        try {
            Message message = Message.builder()
                    .setToken(deviceToken)
                    .putData("transaction_id", transactionCode.toString())
                    .putData("type", "SILENT_SYNC")
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("FCM Silent Push dispatched successfully. Message ID: {}, Transaction ID: {}", messageId, transactionCode);
        } catch (Exception e) {
            log.error("Failed to send FCM Silent Push for Transaction ID {}: {}", transactionCode, e.getMessage(), e);
        }
    }
}