package com.jo.payonservices.auth.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    public RefreshTokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveRefreshToken(String token, String username) {
        redisTemplate.opsForValue().set(token, username, Duration.ofDays(30));
    }

    public String getUsernameFromToken(String token) {
        return redisTemplate.opsForValue().get(token);
    }

    public void deleteRefreshToken(String token) {
        redisTemplate.delete(token);
    }
}