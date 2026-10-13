package com.jo.payonservices.notifications.repository;

import com.jo.payonservices.notifications.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationLogRepo extends MongoRepository<NotificationLog, String> {
    Page<NotificationLog> findByUsernameOrderByTimestampDesc(String username, Pageable pageable);
    Optional<Long> countByUsernameAndIsReadFalse(String username);
    Optional<List<NotificationLog>> findByUsernameAndIsReadFalse(String username);
    Optional<NotificationLog> findByUsernameAndIdAndIsReadFalse(String username, String id);
}
