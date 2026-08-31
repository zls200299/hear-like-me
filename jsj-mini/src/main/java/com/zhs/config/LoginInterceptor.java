package com.zhs.config;

import com.zhs.common.NoLoginRequest;
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
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        NoLoginRequest methodAnnotation = handlerMethod.getMethodAnnotation(NoLoginRequest.class);
        NoLoginRequest classAnnotation = handlerMethod.getBeanType().getAnnotation(NoLoginRequest.class);

        if (methodAnnotation != null || classAnnotation != null) {
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
