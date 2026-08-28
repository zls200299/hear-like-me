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
 * 内置示例音
 *
 * @author 
 * @since 2026-08-28
 */

@Data
@ApiModel
public class SampleAudio implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "vowel/tone/melody")
    private String sampleCode;

    @ApiModelProperty(value = "中文名")
    private String nameCn;

    @ApiModelProperty(value = "英文名")
    private String nameEn;

    @ApiModelProperty(value = "中文说明")
    private String descriptionCn;

    @ApiModelProperty(value = "英文说明")
    private String descriptionEn;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "预生成音频文件；Python 运行时生成时可为空")
    private Long assetId;


    @ApiModelProperty(value = "PREGENERATED/PYTHON_GENERATED")
    private String generatorType;

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
