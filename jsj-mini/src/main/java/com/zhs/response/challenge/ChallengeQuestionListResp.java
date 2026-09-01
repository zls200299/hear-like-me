package com.zhs.response.challenge;

import lombok.Data;

import java.util.List;

/**
 * 听音挑战题目列表
 */
@Data
public class ChallengeQuestionListResp {

    private Integer total;

    private List<ChallengeQuestionItemResp> items;
}
