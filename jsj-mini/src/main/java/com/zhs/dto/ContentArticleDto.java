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
@ApiModel(value = "ContentArticleDto")
public class ContentArticleDto implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "分类ID")
        private Long categoryId;

        @ApiModelProperty(value = "页面/文章唯一标识", required = true)
        private String slug;

        @ApiModelProperty(value = "zh-CN/en-US", required = true)
        private String lang;

        @ApiModelProperty(value = "标题", required = true)
        private String title;

        @ApiModelProperty(value = "副标题")
        private String subtitle;

        @ApiModelProperty(value = "摘要")
        private String summary;

        @ApiModelProperty(value = "MARKDOWN/HTML/JSON", required = true)
        private String contentFormat;

        @ApiModelProperty(value = "正文", required = true)
        private Object contentBody;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "封面/插图文件ID")
        private Long coverAssetId;

        @ApiModelProperty(value = "DRAFT/PUBLISHED/OFFLINE", required = true)
        private String status;

        @ApiModelProperty(value = "排序", required = true)
        private Integer sortOrder;

        @ApiModelProperty(value = "发布时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date publishedTime;

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
