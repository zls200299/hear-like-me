package com.zhs.controller.api;

import com.zhs.common.NoLoginRequest;
import com.zhs.request.challenge.ChallengeSubmitAnswerReq;
import com.zhs.response.challenge.ChallengeQuestionDetailResp;
import com.zhs.response.challenge.ChallengeQuestionListResp;
import com.zhs.response.challenge.ChallengeSubmitAnswerResp;
import com.zhs.service.HearingChallengeService;
import com.zhs.util.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 听音挑战 API
 */
@RestController
@RequestMapping("/api/challenge")
@NoLoginRequest
public class ChallengeController {

    @Resource
    private HearingChallengeService hearingChallengeService;

    /**
     * 拉取已发布题目列表
     */
    @GetMapping("/questions")
    public R<ChallengeQuestionListResp> listQuestions() {
        return R.ok(hearingChallengeService.listPublishedQuestions());
    }

    /**
     * 按序号获取当前题（index 从 1 开始，默认 1）
     */
    @GetMapping("/questions/current")
    public R<ChallengeQuestionDetailResp> getCurrentQuestion(@RequestParam(value = "index", required = false) Integer index) {
        return R.ok(hearingChallengeService.getCurrentQuestion(index));
    }

    /**
     * 按 ID 获取题目详情
     */
    @GetMapping("/questions/{questionId}")
    public R<ChallengeQuestionDetailResp> getQuestionById(@PathVariable("questionId") Long questionId) {
        return R.ok(hearingChallengeService.getQuestionById(questionId));
    }

    /**
     * 提交答案
     */
    @PostMapping("/answer")
    public R<ChallengeSubmitAnswerResp> submitAnswer(@RequestBody ChallengeSubmitAnswerReq req) {
        if (req == null) {
            return R.fail("请求体不能为空");
        }
        return R.ok(hearingChallengeService.submitAnswer(req.getQuestionId(), req.getSelectedChannels()));
    }
}
