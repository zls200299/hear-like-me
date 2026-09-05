package com.zhs.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
 * 音频处理任务/历史记录
 *
 * @author 
 * @since 2026-08-28
 */

@Data
@ApiModel
public class AudioProcessingTask implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "对外任务编号 UUID/雪花ID")
    private String taskNo;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属用户；一期未登录可为空")
    private Long userId;


    @ApiModelProperty(value = "SAMPLE/UPLOAD/RECORDING/READ_ALOUD")
    private String sourceType;

    @ApiModelProperty(value = "source_type=SAMPLE 时使用")
    private String sampleCode;

    @ApiModelProperty(value = "场景代码；自定义参数时可为空")
    private String scenarioCode;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "原始输入文件；内置 Python 生成示例时可为空")
    private Long sourceAssetId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "FFmpeg 标准化后的中间 WAV")
    private Long normalizedAssetId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "Cochlear Vocoder 输出文件")
    private Long outputAssetId;


    @ApiModelProperty(value = "有效通道数 1-22")
    @JsonProperty("nChannels")
    private Integer nChannels;

    @ApiModelProperty(value = "noise/sine")
    private String carrier;

    @ApiModelProperty(value = "Hz")
    @JsonProperty("fLo")
    private java.math.BigDecimal fLo;

    @ApiModelProperty(value = "Hz")
    @JsonProperty("fHi")
    private java.math.BigDecimal fHi;

    @ApiModelProperty(value = "Hz")
    private java.math.BigDecimal envCut;

    @ApiModelProperty(value = "0-1")
    private java.math.BigDecimal spread;

    @ApiModelProperty(value = "0-1")
    private java.math.BigDecimal noiseLevel;

    @ApiModelProperty(value = "包络增益")
    private java.math.BigDecimal envAmp;

    @ApiModelProperty(value = "处理后声音混合比例 0-1")
    private java.math.BigDecimal wetMix;

    @ApiModelProperty(value = "是否启用动态压缩")
    private Integer compressEnabled;

    @ApiModelProperty(value = "归一化峰值；NULL=关闭")
    private java.math.BigDecimal normalizePeak;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "随机种子，便于复现实验")
    private Long randomSeed;


    @ApiModelProperty(value = "算法版本")
    private String algorithmVersion;

    @ApiModelProperty(value = "PENDING/PROCESSING/SUCCESS/FAILED/CANCELLED")
    private String taskStatus;

    @ApiModelProperty(value = "0-100")
    private Integer progress;

    @ApiModelProperty(value = "重试次数")
    private Integer retryCount;

    @ApiModelProperty(value = "清晰度参考分 0-100")
    private Integer clarityScore;

    @ApiModelProperty(value = "几乎听不懂/很吃力/大致能懂/比较清楚/接近清晰")
    private String clarityGrade;

    @ApiModelProperty(value = "频谱细节参考 0-1")
    private java.math.BigDecimal spectralScore;

    @ApiModelProperty(value = "音高线索参考 0-1")
    private java.math.BigDecimal pitchScore;

    @ApiModelProperty(value = "噪声余量参考 0-1")
    private java.math.BigDecimal noiseMargin;

    @ApiModelProperty(value = "错误码")
    private String errorCode;

    @ApiModelProperty(value = "错误信息")
    private String errorMessage;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "排队等待毫秒")
    private Long queueWaitMs;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "实际处理耗时毫秒")
    private Long processingMs;


    @ApiModelProperty(value = "处理开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date processingStartedTime;


    @ApiModelProperty(value = "处理结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date processingFinishedTime;


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


    @ApiModelProperty(value = "逻辑删除；可用于用户删除历史")
    private Integer isDelete;


//===========================自定义字段=================================

}
