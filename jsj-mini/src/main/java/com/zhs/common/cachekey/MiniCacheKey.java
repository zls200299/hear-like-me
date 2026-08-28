package com.zhs.common.cachekey;

import java.util.concurrent.TimeUnit;

/**
 * Redis CacheKey 抽象接口
 * 所有业务 CacheKey 实现此接口，统一 key 构建、过期时间管理
 */
public interface MiniCacheKey {

    /**
     * 构建完整的 Redis key
     */
    String buildKey();

    /**
     * 过期时间，<=0 表示永不过期
     */
    long getExpireTime();

    /**
     * 过期时间单位
     */
    TimeUnit getExpireTimeUnit();
}
