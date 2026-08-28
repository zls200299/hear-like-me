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
@ApiModel(value = "SampleAudioResp")
public class SampleAudioResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "vowel/tone/melody", required = true)
        private String sampleCode;

        @ApiModelProperty(value = "中文名", required = true)
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

        @ApiModelProperty(value = "PREGENERATED/PYTHON_GENERATED", required = true)
        private String generatorType;

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
