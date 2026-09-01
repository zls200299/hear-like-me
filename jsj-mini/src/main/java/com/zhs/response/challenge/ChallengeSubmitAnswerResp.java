package com.zhs.response.challenge;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 听音挑战提交答案结果
 */
@Data
public class ChallengeSubmitAnswerResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    private Integer selectedChannels;

    private Integer correctChannels;

    private Boolean correct;

    private String tip;

    private Boolean hasNext;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long nextQuestionId;

    private Integer nextIndex;

    private Integer total;
}
