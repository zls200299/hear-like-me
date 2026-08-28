package com.zhs.common;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 从请求中解析登录 token（与小程序、Web 客户端约定一致）。
 */
public final class TokenUtil {

    private TokenUtil() {
    }

    public static String resolveFrom(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && !auth.isBlank()) {
            auth = auth.trim();
            if (auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return auth.substring(7).trim();
            }
            return auth;
        }
        String t = request.getHeader("token");
        if (t != null && !t.isBlank()) {
            return t.trim();
        }
        t = request.getHeader("access-token");
        if (t != null && !t.isBlank()) {
            return t.trim();
        }
        return null;
    }
}
