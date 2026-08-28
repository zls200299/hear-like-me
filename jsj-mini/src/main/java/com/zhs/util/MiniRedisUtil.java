package com.zhs.util;

import com.zhs.common.RedisCache;
import com.zhs.common.cachekey.MiniCacheKey;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 小程序 Redis 工具类
 * <p>
 * 桥接 MiniCacheKey 与底层 RedisCache，业务层统一走此工具类操作 Redis。
 * RedisCache 保持纯粹，不感知任何业务 CacheKey。
 */
@Component
public class MiniRedisUtil {

    @Resource
    private RedisCache redisCache;

    /**
     * 缓存对象（自动使用 CacheKey 中的 key 和过期时间）
     */
    public <T> void set(final MiniCacheKey cacheKey, final T value) {
        if (cacheKey.getExpireTime() > 0) {
            redisCache.setCacheObject(cacheKey.buildKey(), value,
                    cacheKey.getExpireTime(), cacheKey.getExpireTimeUnit());
        } else {
            redisCache.setCacheObject(cacheKey.buildKey(), value);
        }
    }

    /**
     * 获取缓存对象
     */
    public <T> T get(final MiniCacheKey cacheKey) {
        return redisCache.getCacheObject(cacheKey.buildKey());
    }

    /**
     * 删除缓存对象
     */
    public boolean delete(final MiniCacheKey cacheKey) {
        return redisCache.deleteObject(cacheKey.buildKey());
    }

    /**
     * 判断 key 是否存在
     */
    public Boolean hasKey(final MiniCacheKey cacheKey) {
        return redisCache.hasKey(cacheKey.buildKey());
    }

    /**
     * 刷新过期时间
     */
    public boolean expire(final MiniCacheKey cacheKey) {
        return redisCache.expire(cacheKey.buildKey(), cacheKey.getExpireTime(), cacheKey.getExpireTimeUnit());
    }
}
