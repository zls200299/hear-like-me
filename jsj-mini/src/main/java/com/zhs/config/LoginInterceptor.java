package com.zhs.config;

import com.zhs.common.NoLoginRequest;
import com.zhs.common.RequireLogin;
import com.zhs.common.TokenUtil;
import com.zhs.common.UserContext;
import com.zhs.common.cachekey.user.TokenCacheKey;
import com.zhs.util.MiniRedisUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private MiniRedisUtil miniRedisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (!mustLogin(handlerMethod)) {
            // 免登录接口：有有效 token 时仍写入 UserContext，便于落库归属用户
            try {
                tryBindUserContext(request);
            } catch (Exception ignored) {
                // Redis 异常不得阻断免登录业务（上传/预览/示例音等）
            }
            return true;
        }

        String token = TokenUtil.resolveFrom(request);
        if (token == null || token.isEmpty()) {
            writeUnauthorized(response, "未登录");
            return false;
        }

        Long userId = resolveUserId(miniRedisUtil.get(new TokenCacheKey(token)));
        if (userId == null) {
            writeUnauthorized(response, "登录已过期");
            return false;
        }

        UserContext.setUserId(userId);
        return true;
    }

    /**
     * 判定是否必须登录。优先级：
     * 1. 方法 {@link RequireLogin} → 必须登录
     * 2. 方法 {@link NoLoginRequest} → 免登录
     * 3. 类 {@link RequireLogin} → 必须登录
     * 4. 类 {@link NoLoginRequest} → 免登录
     * 5. 默认 → 必须登录
     */
    private boolean mustLogin(HandlerMethod handlerMethod) {
        if (handlerMethod.getMethodAnnotation(RequireLogin.class) != null) {
            return true;
        }
        if (handlerMethod.getMethodAnnotation(NoLoginRequest.class) != null) {
            return false;
        }
        Class<?> beanType = handlerMethod.getBeanType();
        if (beanType.getAnnotation(RequireLogin.class) != null) {
            return true;
        }
        if (beanType.getAnnotation(NoLoginRequest.class) != null) {
            return false;
        }
        return true;
    }

    private void tryBindUserContext(HttpServletRequest request) {
        String token = TokenUtil.resolveFrom(request);
        if (token == null || token.isEmpty()) {
            return;
        }
        Long userId = resolveUserId(miniRedisUtil.get(new TokenCacheKey(token)));
        if (userId != null) {
            UserContext.setUserId(userId);
        }
    }

    private Long resolveUserId(Object cached) {
        if (cached == null) {
            return null;
        }
        if (cached instanceof Long value) {
            return value;
        }
        if (cached instanceof Integer value) {
            return value.longValue();
        }
        if (cached instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + message + "\"}");
    }
}
