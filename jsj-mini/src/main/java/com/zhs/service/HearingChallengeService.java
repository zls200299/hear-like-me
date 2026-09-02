package com.zhs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.model.HearingChallenge;
import com.zhs.dto.HearingChallengeDto;
import com.zhs.response.challenge.ChallengeQuestionDetailResp;
import com.zhs.response.challenge.ChallengeQuestionListResp;
import com.zhs.response.challenge.ChallengeSubmitAnswerResp;
import com.zhs.util.R;

public interface HearingChallengeService extends IService<HearingChallenge> {

    R addOrUpdate(HearingChallengeDto dto);

    ChallengeQuestionListResp listPublishedQuestions();

    ChallengeQuestionDetailResp getCurrentQuestion(Integer index);

    ChallengeQuestionDetailResp getQuestionById(Long questionId);

    ChallengeSubmitAnswerResp submitAnswer(Long questionId, Integer selectedChannels);
}
