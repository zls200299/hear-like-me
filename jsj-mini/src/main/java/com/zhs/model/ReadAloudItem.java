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
 * 图片点读内容
 *
 * @author 
 * @since 2026-08-28
 */

@Data
@ApiModel
public class ReadAloudItem implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "分类ID")
    private Long categoryId;


    @ApiModelProperty(value = "唯一业务编码")
    private String itemCode;

    @ApiModelProperty(value = "中文标题")
    private String titleCn;

    @ApiModelProperty(value = "英文标题")
    private String titleEn;

    @ApiModelProperty(value = "点读中文文字，如“苹果”")
    private String speechTextCn;

    @ApiModelProperty(value = "中文说明")
    private String descriptionCn;

    @ApiModelProperty(value = "英文说明")
    private String descriptionEn;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "点读图片")
    private Long imageAssetId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "普通中文点读音频")
    private Long audioAssetId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "预生成的人工耳蜗模拟音频；可为空")
    private Long processedAudioAssetId;


    @ApiModelProperty(value = "ORIGINAL/PROCESSED/BOTH")
    private String playMode;

    @ApiModelProperty(value = "需要模拟音时默认使用的场景代码")
    private String defaultScenarioCode;

    @ApiModelProperty(value = "DRAFT/PUBLISHED/OFFLINE")
    private String status;

    @ApiModelProperty(value = "排序")
    private Integer sortOrder;

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
