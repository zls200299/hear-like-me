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
 * 科普文章/页面
 *
 * @author 
 * @since 2026-08-28
 */

@Data
@ApiModel
public class ContentArticle implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "分类ID")
    private Long categoryId;


    @ApiModelProperty(value = "页面/文章唯一标识")
    private String slug;

    @ApiModelProperty(value = "zh-CN/en-US")
    private String lang;

    @ApiModelProperty(value = "标题")
    private String title;

    @ApiModelProperty(value = "副标题")
    private String subtitle;

    @ApiModelProperty(value = "摘要")
    private String summary;

    @ApiModelProperty(value = "MARKDOWN/HTML/JSON")
    private String contentFormat;

    @ApiModelProperty(value = "正文")
    private Object contentBody;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "封面/插图文件ID")
    private Long coverAssetId;


    @ApiModelProperty(value = "DRAFT/PUBLISHED/OFFLINE")
    private String status;

    @ApiModelProperty(value = "排序")
    private Integer sortOrder;

    @ApiModelProperty(value = "发布时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publishedTime;


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
