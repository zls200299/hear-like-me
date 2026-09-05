package com.zhs.common;

import com.zhs.exception.ServiceException;

public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 获取当前登录用户 ID；未登录时抛出 401。
     * 供业务层在必须登录的接口中统一取用户，避免各处手写判空。
     */
    public static Long requireUserId() {
        Long userId = USER_ID_HOLDER.get();
        if (userId == null) {
            throw new ServiceException("未登录", 401);
        }
        return userId;
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
