package com.window.global.security.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;


@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final RedisTemplate<Object, Object> redisTemplate;

    @Value("${jwt.refresh.token.expire}")
    private long refreshTokenExpire;

    private static final String REFRESH_TOKEN_KEY = "refresh";

    public void saveRefreshToken(Long id, String refreshToken) {
        ValueOperations<Object, Object> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(REFRESH_TOKEN_KEY + ":" + id, refreshToken, Duration.ofHours(refreshTokenExpire));
    }

    public boolean existsRefreshToken(Long id) {
        ValueOperations<Object, Object> valueOperations = redisTemplate.opsForValue();
        return valueOperations.get(REFRESH_TOKEN_KEY + ":" + id) != null;
    }

    public void deleteRefreshToken(Long id) {
        redisTemplate.delete(REFRESH_TOKEN_KEY + ":" + id);
    }

    public String getRefreshToken(Long id) {
        ValueOperations<Object, Object> valueOperations = redisTemplate.opsForValue();
        return (String) valueOperations.get(REFRESH_TOKEN_KEY + ":" + id);
    }
}
