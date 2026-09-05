package com.zhs.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.dao.HearingChallengeAttemptDao;
import com.zhs.model.HearingChallengeAttempt;
import com.zhs.response.challenge.ChallengeStatsResp;
import com.zhs.service.HearingChallengeAttemptService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HearingChallengeAttemptServiceImpl
        extends ServiceImpl<HearingChallengeAttemptDao, HearingChallengeAttempt>
        implements HearingChallengeAttemptService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public ChallengeStatsResp getStats(Integer days) {
        int trendDays = (days != null && days == 30) ? 30 : 7;
        LocalDate today = LocalDate.now(ZONE);
        Date sinceTrend = Date.from(today.minusDays(trendDays - 1L).atStartOfDay(ZONE).toInstant());
        Date since7 = Date.from(today.minusDays(6L).atStartOfDay(ZONE).toInstant());

        ChallengeStatsResp resp = new ChallengeStatsResp();
        fillSummary(resp, since7);
        fillTrend(resp, sinceTrend, today, trendDays);
        fillQuestionStats(resp);
        fillTopUsers(resp);
        return resp;
    }

    private void fillSummary(ChallengeStatsResp resp, Date since7) {
        Map<String, Object> row = baseMapper.selectSummary(since7);
        ChallengeStatsResp.ChallengeStatsSummary summary = resp.getSummary();
        if (row == null || row.isEmpty()) {
            return;
        }
        long total = toLong(mapGet(row, "totalAttempts"));
        long correct = toLong(mapGet(row, "correctCount"));
        summary.setTotalAttempts(total);
        summary.setUniqueUsers(toLong(mapGet(row, "uniqueUsers")));
        summary.setCorrectCount(correct);
        summary.setAttemptsLast7Days(toLong(mapGet(row, "attemptsLast7Days")));
        summary.setCorrectRate(rate(correct, total));
    }

    private void fillTrend(ChallengeStatsResp resp, Date since, LocalDate today, int trendDays) {
        List<Map<String, Object>> rows = baseMapper.selectDailyTrend(since);
        Map<String, long[]> byDay = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object dayObj = mapGet(row, "day");
                String day = dayObj == null ? null : String.valueOf(dayObj);
                if (day == null || day.isBlank()) {
                    continue;
                }
                byDay.put(day, new long[]{toLong(mapGet(row, "attempts")), toLong(mapGet(row, "correctCount"))});
            }
        }

        List<ChallengeStatsResp.ChallengeStatsTrendPoint> trend = new ArrayList<>(trendDays);
        LocalDate start = today.minusDays(trendDays - 1L);
        for (int i = 0; i < trendDays; i++) {
            LocalDate day = start.plusDays(i);
            String key = day.format(DAY_FMT);
            long[] values = byDay.getOrDefault(key, new long[]{0L, 0L});
            ChallengeStatsResp.ChallengeStatsTrendPoint point = new ChallengeStatsResp.ChallengeStatsTrendPoint();
            point.setDate(key);
            point.setAttempts(values[0]);
            point.setCorrect(values[1]);
            trend.add(point);
        }
        resp.setTrend(trend);
    }

    private void fillQuestionStats(ChallengeStatsResp resp) {
        List<Map<String, Object>> rows = baseMapper.selectQuestionStats(15);
        List<ChallengeStatsResp.ChallengeStatsQuestionItem> list = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                long attempts = toLong(mapGet(row, "attempts"));
                long correct = toLong(mapGet(row, "correctCount"));
                ChallengeStatsResp.ChallengeStatsQuestionItem item = new ChallengeStatsResp.ChallengeStatsQuestionItem();
                item.setQuestionId(toLongObj(mapGet(row, "questionId")));
                item.setQuestionTitle(asText(mapGet(row, "questionTitle")));
                item.setAudioTitle(asText(mapGet(row, "audioTitle")));
                item.setAttempts(attempts);
                item.setCorrect(correct);
                item.setCorrectRate(rate(correct, attempts));
                list.add(item);
            }
        }
        resp.setQuestionStats(list);
    }

    private void fillTopUsers(ChallengeStatsResp resp) {
        List<Map<String, Object>> rows = baseMapper.selectTopUsers(10);
        List<ChallengeStatsResp.ChallengeStatsUserItem> list = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                long attempts = toLong(mapGet(row, "attempts"));
                long correct = toLong(mapGet(row, "correctCount"));
                ChallengeStatsResp.ChallengeStatsUserItem item = new ChallengeStatsResp.ChallengeStatsUserItem();
                item.setUserId(toLongObj(mapGet(row, "userId")));
                String nickname = asText(mapGet(row, "userNickname"));
                item.setUserNickname(nickname == null || nickname.isBlank() ? "未设置昵称" : nickname);
                item.setAttempts(attempts);
                item.setCorrect(correct);
                item.setCorrectRate(rate(correct, attempts));
                list.add(item);
            }
        }
        resp.setTopUsers(list);
    }

    private static Object mapGet(Map<String, Object> row, String key) {
        if (row == null || key == null) {
            return null;
        }
        if (row.containsKey(key)) {
            return row.get(key);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static double rate(long correct, long total) {
        if (total <= 0) {
            return 0D;
        }
        return BigDecimal.valueOf(correct * 100.0 / total)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static Long toLongObj(Object value) {
        if (value == null) {
            return null;
        }
        long parsed = toLong(value);
        return parsed;
    }

    private static String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
