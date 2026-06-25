package com.jo.payonservices.notifications.repository;

import com.jo.payonservices.notifications.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationLogRepo extends MongoRepository<NotificationLog, String> {
    List<NotificationLog> findByUsername(String username);
    Page<NotificationLog> findByUsernameOrderByTimestampDesc(String username, Pageable pageable);
}
