package com.zhs.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 听音挑战答题记录
 */
@Data
@TableName("hearing_challenge_attempt")
public class HearingChallengeAttempt implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String userNickname;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    private String questionCode;

    private String questionTitle;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long audioBankId;

    private String audioTitle;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long audioAssetId;

    @JsonProperty("selectedChannels")
    private Integer selectedChannels;

    @JsonProperty("correctChannels")
    private Integer correctChannels;

    @JsonProperty("isCorrect")
    private Integer isCorrect;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
