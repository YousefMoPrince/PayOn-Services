package com.jo.payonservices.notifications.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${app.firebase.config-path:firebase-service-account.json}")
    private String firebaseConfigPath;

    @PostConstruct
    public void initializeFirebase() {
        try {
            InputStream serviceAccount = new ClassPathResource(firebaseConfigPath).getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Application initialized successfully with credentials from {}", firebaseConfigPath);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Application: {}", e.getMessage(), e);
        }
    }
}
