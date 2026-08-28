package com.zhs.dto;

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
@ApiModel(value = "ReadAloudItemDto")
public class ReadAloudItemDto implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "分类ID")
        private Long categoryId;

        @ApiModelProperty(value = "唯一业务编码", required = true)
        private String itemCode;

        @ApiModelProperty(value = "中文标题", required = true)
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

        @ApiModelProperty(value = "ORIGINAL/PROCESSED/BOTH", required = true)
        private String playMode;

        @ApiModelProperty(value = "需要模拟音时默认使用的场景代码")
        private String defaultScenarioCode;

        @ApiModelProperty(value = "DRAFT/PUBLISHED/OFFLINE", required = true)
        private String status;

        @ApiModelProperty(value = "排序", required = true)
        private Integer sortOrder;

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
