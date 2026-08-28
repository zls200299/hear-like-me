package com.zhs.common.cachekey.user;

import com.zhs.common.cachekey.MiniCacheKey;

import java.util.concurrent.TimeUnit;

/**
 * 登录 Token 的 CacheKey
 * key 格式：mini:token:{token}
 * 过期时间：30 天
 */
public class TokenCacheKey implements MiniCacheKey {

    private static final String PREFIX = "mini:token:";
    private static final long EXPIRE_DAYS = 30;

    private final String token;

    public TokenCacheKey(String token) {
        this.token = token;
    }

    @Override
    public String buildKey() {
        return PREFIX + token;
    }

    @Override
    public long getExpireTime() {
        return EXPIRE_DAYS;
    }

    @Override
    public TimeUnit getExpireTimeUnit() {
        return TimeUnit.DAYS;
    }
}
