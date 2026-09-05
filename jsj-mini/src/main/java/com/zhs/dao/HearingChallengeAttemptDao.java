package com.zhs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.model.HearingChallengeAttempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface HearingChallengeAttemptDao extends BaseMapper<HearingChallengeAttempt> {

    @Select("""
            SELECT
              COUNT(*) AS totalAttempts,
              COUNT(DISTINCT user_id) AS uniqueUsers,
              IFNULL(SUM(is_correct), 0) AS correctCount,
              SUM(CASE WHEN create_time >= #{since7Days} THEN 1 ELSE 0 END) AS attemptsLast7Days
            FROM hearing_challenge_attempt
            """)
    Map<String, Object> selectSummary(@Param("since7Days") Date since7Days);

    @Select("""
            SELECT
              DATE_FORMAT(create_time, '%Y-%m-%d') AS day,
              COUNT(*) AS attempts,
              IFNULL(SUM(is_correct), 0) AS correctCount
            FROM hearing_challenge_attempt
            WHERE create_time >= #{since}
            GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
            ORDER BY day ASC
            """)
    List<Map<String, Object>> selectDailyTrend(@Param("since") Date since);

    @Select("""
            SELECT
              question_id AS questionId,
              MAX(question_title) AS questionTitle,
              MAX(audio_title) AS audioTitle,
              COUNT(*) AS attempts,
              IFNULL(SUM(is_correct), 0) AS correctCount
            FROM hearing_challenge_attempt
            GROUP BY question_id
            ORDER BY attempts DESC, question_id ASC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> selectQuestionStats(@Param("limit") int limit);

    @Select("""
            SELECT
              user_id AS userId,
              MAX(user_nickname) AS userNickname,
              COUNT(*) AS attempts,
              IFNULL(SUM(is_correct), 0) AS correctCount
            FROM hearing_challenge_attempt
            GROUP BY user_id
            ORDER BY attempts DESC, user_id ASC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> selectTopUsers(@Param("limit") int limit);
}
