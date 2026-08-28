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
 * 音频处理任务事件日志
 *
 * @author 
 * @since 2026-08-28
 */

@Data
@ApiModel
public class AudioProcessingTaskEvent implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "任务ID")
    private Long taskId;


    @ApiModelProperty(value = "CREATED/NORMALIZING/PROCESSING/SUCCESS/FAILED/RETRY/CANCELLED")
    private String eventType;

    @ApiModelProperty(value = "UPLOAD/FFMPEG/VOCODER/STORAGE/OTHER")
    private String stage;

    @ApiModelProperty(value = "0-100")
    private Integer progress;

    @ApiModelProperty(value = "事件描述")
    private String message;

    @ApiModelProperty(value = "扩展数据")
    private JsonObject detailJson;

    @ApiModelProperty(value = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;



//===========================自定义字段=================================

}
