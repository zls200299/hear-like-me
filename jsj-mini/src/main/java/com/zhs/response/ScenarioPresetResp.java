package com.zhs.response;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 *
 * @author 
 * @since 2026-08-28
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "ScenarioPresetResp")
public class ScenarioPresetResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "quiet/restaurant/phone/music/tone/minimal", required = true)
        private String scenarioCode;

        @ApiModelProperty(value = "中文名", required = true)
        private String nameCn;

        @ApiModelProperty(value = "英文名")
        private String nameEn;

        @ApiModelProperty(value = "中文说明")
        private String descriptionCn;

        @ApiModelProperty(value = "英文说明")
        private String descriptionEn;

        @ApiModelProperty(value = "图标 key")
        private String icon;

        @ApiModelProperty(value = "有效通道数 1-22", required = true)
        private Integer nChannels;

        @ApiModelProperty(value = "noise/sine", required = true)
        private String carrier;

        @ApiModelProperty(value = "频率下限 Hz", required = true)
        private java.math.BigDecimal fLo;

        @ApiModelProperty(value = "频率上限 Hz", required = true)
        private java.math.BigDecimal fHi;

        @ApiModelProperty(value = "包络低通截止 Hz", required = true)
        private java.math.BigDecimal envCut;

        @ApiModelProperty(value = "电流扩散 0-1", required = true)
        private java.math.BigDecimal spread;

        @ApiModelProperty(value = "背景噪声 0-1", required = true)
        private java.math.BigDecimal noiseLevel;

        @ApiModelProperty(value = "包络增益", required = true)
        private java.math.BigDecimal envAmp;

        @ApiModelProperty(value = "处理后声音混合比例 0-1", required = true)
        private java.math.BigDecimal wetMix;

        @ApiModelProperty(value = "是否启用动态压缩", required = true)
        private Integer compressEnabled;

        @ApiModelProperty(value = "归一化峰值；NULL=关闭")
        private java.math.BigDecimal normalizePeak;

        @ApiModelProperty(value = "vowel/tone/melody")
        private String defaultSampleCode;

        @ApiModelProperty(value = "排序", required = true)
        private Integer sortOrder;

        @ApiModelProperty(value = "是否启用", required = true)
        private Integer enabled;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiModelProperty(value = "更新时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        @ApiModelProperty(value = "逻辑删除", required = true)
        private Integer isDelete;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
