package com.zhs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.model.HearingChallenge;
import com.zhs.response.challenge.ChallengeQuestionDetailResp;
import com.zhs.response.challenge.ChallengeQuestionListResp;
import com.zhs.response.challenge.ChallengeSubmitAnswerResp;

public interface HearingChallengeService extends IService<HearingChallenge> {

    ChallengeQuestionListResp listPublishedQuestions();

    ChallengeQuestionDetailResp getCurrentQuestion(Integer index);

    ChallengeQuestionDetailResp getQuestionById(Long questionId);

    ChallengeSubmitAnswerResp submitAnswer(Long questionId, Integer selectedChannels);
}
