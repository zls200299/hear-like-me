package com.zhs.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 根据 last_active_time 生成轻量「活跃状态」文案
 */
public final class ActiveStatusUtil {

    private ActiveStatusUtil() {
    }

    public static String format(LocalDateTime lastActive) {
        if (lastActive == null) {
            return "最近在线";
        }
        LocalDateTime now = LocalDateTime.now();
        if (lastActive.isAfter(now)) {
            return "刚刚在线";
        }
        long minutes = Duration.between(lastActive, now).toMinutes();
        if (minutes < 5) {
            return "刚刚在线";
        }
        if (minutes < 60) {
            return minutes + "分钟前活跃";
        }
        if (ChronoUnit.DAYS.between(lastActive.toLocalDate(), now.toLocalDate()) == 0) {
            return "今天活跃";
        }
        if (ChronoUnit.DAYS.between(lastActive.toLocalDate(), now.toLocalDate()) == 1) {
            return "昨天活跃";
        }
        return "最近在线";
    }
}
