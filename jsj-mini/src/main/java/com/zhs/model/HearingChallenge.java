package com.zhs.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 听音挑战题库
 */
@Data
@TableName("hearing_challenge")
public class HearingChallenge implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String questionCode;

    private String title;

    private String description;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long audioAssetId;

    private Integer nChannels;

    private String carrier;

    private BigDecimal fLo;

    private BigDecimal fHi;

    private BigDecimal envCut;

    private BigDecimal spread;

    private BigDecimal noiseLevel;

    private BigDecimal envAmp;

    private BigDecimal wetMix;

    private Integer compressEnabled;

    private BigDecimal normalizePeak;

    private String correctTip;

    private String wrongTip;

    private String status;

    private Integer sortOrder;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publishedTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    private Integer isDelete;
}
