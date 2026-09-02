package com.zhs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "HearingChallengeDto")
public class HearingChallengeDto implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
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

    private List<String> idList;
}
