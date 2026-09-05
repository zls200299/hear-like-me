package com.zhs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.model.HearingChallengeAttempt;
import com.zhs.response.challenge.ChallengeStatsResp;

public interface HearingChallengeAttemptService extends IService<HearingChallengeAttempt> {

    /**
     * 挑战统计看板
     *
     * @param days 趋势天数，支持 7 / 30
     */
    ChallengeStatsResp getStats(Integer days);
}
