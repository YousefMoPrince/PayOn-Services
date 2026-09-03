package com.jo.payonservices.notifications.repository;

import com.jo.payonservices.notifications.model.UserDeviceToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceTokenRepository extends MongoRepository<UserDeviceToken, String> {
    UserDeviceToken findByUserId(UUID userId);
    void deleteByDeviceToken(String deviceToken);
    void deleteByUserIdAndDeviceToken(UUID userId, String deviceToken);
}