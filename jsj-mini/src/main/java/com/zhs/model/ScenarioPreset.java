package com.zhs.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;


/**
 * 人工耳蜗场景预设
 *
 * @author 
 * @since 2026-08-28
 */

@Data
@ApiModel
public class ScenarioPreset implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "quiet/restaurant/phone/music/tone/minimal")
    private String scenarioCode;

    @ApiModelProperty(value = "中文名")
    private String nameCn;

    @ApiModelProperty(value = "英文名")
    private String nameEn;

    @ApiModelProperty(value = "中文说明")
    private String descriptionCn;

    @ApiModelProperty(value = "英文说明")
    private String descriptionEn;

    @ApiModelProperty(value = "图标 key")
    private String icon;

    @ApiModelProperty(value = "有效通道数 1-22")
    private Integer nChannels;

    @ApiModelProperty(value = "noise/sine")
    private String carrier;

    @ApiModelProperty(value = "频率下限 Hz")
    private java.math.BigDecimal fLo;

    @ApiModelProperty(value = "频率上限 Hz")
    private java.math.BigDecimal fHi;

    @ApiModelProperty(value = "包络低通截止 Hz")
    private java.math.BigDecimal envCut;

    @ApiModelProperty(value = "电流扩散 0-1")
    private java.math.BigDecimal spread;

    @ApiModelProperty(value = "背景噪声 0-1")
    private java.math.BigDecimal noiseLevel;

    @ApiModelProperty(value = "包络增益")
    private java.math.BigDecimal envAmp;

    @ApiModelProperty(value = "处理后声音混合比例 0-1")
    private java.math.BigDecimal wetMix;

    @ApiModelProperty(value = "是否启用动态压缩")
    private Integer compressEnabled;

    @ApiModelProperty(value = "归一化峰值；NULL=关闭")
    private java.math.BigDecimal normalizePeak;

    @ApiModelProperty(value = "vowel/tone/melody")
    private String defaultSampleCode;

    @ApiModelProperty(value = "排序")
    private Integer sortOrder;

    @ApiModelProperty(value = "是否启用")
    private Integer enabled;

    @ApiModelProperty(value = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;


    @ApiModelProperty(value = "更新时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


    @ApiModelProperty(value = "逻辑删除")
    private Integer isDelete;


//===========================自定义字段=================================

}
