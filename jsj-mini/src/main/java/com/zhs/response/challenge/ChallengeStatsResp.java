package com.zhs.response.challenge;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 听音挑战统计看板
 */
@Data
public class ChallengeStatsResp {

    private ChallengeStatsSummary summary = new ChallengeStatsSummary();

    private List<ChallengeStatsTrendPoint> trend = new ArrayList<>();

    private List<ChallengeStatsQuestionItem> questionStats = new ArrayList<>();

    private List<ChallengeStatsUserItem> topUsers = new ArrayList<>();

    @Data
    public static class ChallengeStatsSummary {
        private Long totalAttempts = 0L;
        private Long uniqueUsers = 0L;
        /** 正确率 0-100，保留一位小数 */
        private Double correctRate = 0D;
        private Long attemptsLast7Days = 0L;
        private Long correctCount = 0L;
    }

    @Data
    public static class ChallengeStatsTrendPoint {
        private String date;
        private Long attempts = 0L;
        private Long correct = 0L;
    }

    @Data
    public static class ChallengeStatsQuestionItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long questionId;
        private String questionTitle;
        private String audioTitle;
        private Long attempts = 0L;
        private Long correct = 0L;
        private Double correctRate = 0D;
    }

    @Data
    public static class ChallengeStatsUserItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long userId;
        private String userNickname;
        private Long attempts = 0L;
        private Long correct = 0L;
        private Double correctRate = 0D;
    }
}
