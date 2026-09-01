package com.zhs.request.challenge;

import lombok.Data;

/**
 * 听音挑战提交答案
 */
@Data
public class ChallengeSubmitAnswerReq {

    /** 题目 ID */
    private Long questionId;

    /** 用户选择的有效通道数：2/4/8/16 */
    private Integer selectedChannels;
}
