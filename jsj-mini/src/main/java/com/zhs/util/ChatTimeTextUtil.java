package com.zhs.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 聊天消息发送时间展示（与前端气泡旁时间风格一致）
 */
public final class ChatTimeTextUtil {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private ChatTimeTextUtil() {
    }

    /**
     * 刚刚 / HH:mm / 昨天 HH:mm / M月d日 HH:mm
     */
    public static String formatSendTimeText(long sendTimeMillis) {
        if (sendTimeMillis <= 0) {
            return "";
        }
        LocalDateTime t = LocalDateTime.ofInstant(Instant.ofEpochMilli(sendTimeMillis), ZoneId.systemDefault());
        LocalDateTime now = LocalDateTime.now();
        if (t.isAfter(now)) {
            return "刚刚";
        }
        long minutes = Duration.between(t, now).toMinutes();
        if (minutes < 1) {
            return "刚刚";
        }
        if (ChronoUnit.DAYS.between(t.toLocalDate(), now.toLocalDate()) == 0) {
            return t.format(HM);
        }
        if (ChronoUnit.DAYS.between(t.toLocalDate(), now.toLocalDate()) == 1) {
            return "昨天 " + t.format(HM);
        }
        if (t.getYear() == now.getYear()) {
            return t.getMonthValue() + "月" + t.getDayOfMonth() + "日 " + t.format(HM);
        }
        return t.getYear() + "年" + t.getMonthValue() + "月" + t.getDayOfMonth() + "日 " + t.format(HM);
    }
}
