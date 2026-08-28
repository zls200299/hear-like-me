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
@ApiModel(value = "AudioProcessingTaskEventResp")
public class AudioProcessingTaskEventResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "任务ID")
        private Long taskId;

        @ApiModelProperty(value = "CREATED/NORMALIZING/PROCESSING/SUCCESS/FAILED/RETRY/CANCELLED", required = true)
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
        private Date createTime;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
