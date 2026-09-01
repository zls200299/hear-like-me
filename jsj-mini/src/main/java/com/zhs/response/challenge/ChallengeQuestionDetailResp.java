package com.zhs.response.challenge;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 听音挑战当前题目详情（不含正确答案）
 */
@Data
public class ChallengeQuestionDetailResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String questionCode;

    private String title;

    private String description;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long audioAssetId;

    private String audioUrl;

    /** 当前题序号，从 1 开始 */
    private Integer index;

    private Integer total;
}
