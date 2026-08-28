package com.zhs.common;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 底层操作封装，业务层通过 MiniRedisUtil 间接调用
 */
@Component
public class RedisCache {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public <T> void setCacheObject(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public <T> void setCacheObject(String key, T value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @SuppressWarnings("unchecked")
    public <T> T getCacheObject(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    public boolean deleteObject(String key) {
        Boolean result = redisTemplate.delete(key);
        return Boolean.TRUE.equals(result);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        Boolean result = redisTemplate.expire(key, timeout, unit);
        return Boolean.TRUE.equals(result);
    }
}
