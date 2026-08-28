package com.zhs.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
public class AddAudioProcessingTaskReq implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        private String taskNo;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long userId;

        private String sourceType;

        private String sampleCode;

        private String scenarioCode;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long sourceAssetId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long normalizedAssetId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long outputAssetId;

        private Integer nChannels;

        private String carrier;

        private java.math.BigDecimal fLo;

        private java.math.BigDecimal fHi;

        private java.math.BigDecimal envCut;

        private java.math.BigDecimal spread;

        private java.math.BigDecimal noiseLevel;

        private java.math.BigDecimal envAmp;

        private java.math.BigDecimal wetMix;

        private Integer compressEnabled;

        private java.math.BigDecimal normalizePeak;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long randomSeed;

        private String algorithmVersion;

        private String taskStatus;

        private Integer progress;

        private Integer retryCount;

        private Integer clarityScore;

        private String clarityGrade;

        private java.math.BigDecimal spectralScore;

        private java.math.BigDecimal pitchScore;

        private java.math.BigDecimal noiseMargin;

        private String errorCode;

        private String errorMessage;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long queueWaitMs;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long processingMs;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date processingStartedTime;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date processingFinishedTime;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        private Integer isDelete;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
