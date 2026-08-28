package com.zhs.util;

/**
 * 会话 UUID 生成工具
 */
public class SessionUuidUtil {

    /**
     * 生成会话 UUID（小ID_大ID）
     */
    public static String generate(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }

        int compare = userId1.compareTo(userId2);
        if (compare < 0) {
            return userId1 + "_" + userId2;
        } else if (compare > 0) {
            return userId2 + "_" + userId1;
        } else {
            throw new IllegalArgumentException("不能和自己聊天");
        }
    }
}
