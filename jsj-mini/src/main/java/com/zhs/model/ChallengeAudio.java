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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 听音挑战模拟音频库。
 */
@Data
@TableName("challenge_audio_bank")
public class ChallengeAudio implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String audioCode;
    private String title;
    private String description;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceAssetId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long outputAssetId;

    private String processingTaskNo;

    @JsonProperty("nChannels")
    private Integer nChannels;

    private String carrier;

    @JsonProperty("fLo")
    private BigDecimal fLo;

    @JsonProperty("fHi")
    private BigDecimal fHi;

    private BigDecimal envCut;
    private BigDecimal spread;
    private BigDecimal noiseLevel;
    private Integer versionNo;
    private String status;
    private String errorMessage;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date generatedTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    private Integer isDelete;
}
