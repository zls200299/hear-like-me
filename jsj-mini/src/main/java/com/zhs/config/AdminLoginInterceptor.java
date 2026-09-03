package com.zhs.config;

import com.zhs.common.RedisCache;
import com.zhs.common.TokenUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class AdminLoginInterceptor implements HandlerInterceptor {

    private static final String LOGIN_USER_KEY = "login_user_key";

    @Value("${admin.auth.enabled:true}")
    private boolean enabled;

    @Value("${admin.auth.token-secret}")
    private String tokenSecret;

    @Value("${admin.auth.token-cache-prefix:login_tokens:}")
    private String tokenCachePrefix;

    @Resource
    private RedisCache redisCache;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled || !(handler instanceof HandlerMethod)) {
            return true;
        }

        String token = TokenUtil.resolveFrom(request);
        if (token == null || token.isEmpty()) {
            writeUnauthorized(response, "后台登录已过期，请重新登录");
            return false;
        }

        String tokenUuid = parseTokenUuid(token);
        if (tokenUuid == null || tokenUuid.isEmpty() || !Boolean.TRUE.equals(redisCache.hasKey(tokenCachePrefix + tokenUuid))) {
            writeUnauthorized(response, "后台登录已过期，请重新登录");
            return false;
        }

        return true;
    }

    private String parseTokenUuid(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(tokenSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object value = claims.get(LOGIN_USER_KEY);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + message + "\"}");
    }
}
