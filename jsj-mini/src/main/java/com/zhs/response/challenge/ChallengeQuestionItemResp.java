package com.zhs.response.challenge;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 听音挑战题目列表项
 */
@Data
public class ChallengeQuestionItemResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String questionCode;

    private String title;

    private String description;

    private Integer sortOrder;

    /** 题目序号，从 1 开始 */
    private Integer index;
}
