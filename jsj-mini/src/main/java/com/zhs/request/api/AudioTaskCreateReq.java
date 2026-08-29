package com.zhs.request.api;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 创建音频处理任务请求
 *
 * @author
 * @since 2026-08-28
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AudioTaskCreateReq implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceAssetId;

    private String scenarioCode;

    /** SAMPLE / UPLOAD / RECORDING / READ_ALOUD，默认 UPLOAD */
    private String sourceType;
}