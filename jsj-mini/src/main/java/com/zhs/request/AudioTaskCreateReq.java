package com.zhs.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;

/**
 * 创建音频处理任务请求（小程序 API）
 *
 * @author
 * @since 2026-08-28
 */
public class AudioTaskCreateReq implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceAssetId;

    private String scenarioCode;

    /** SAMPLE / UPLOAD / RECORDING / READ_ALOUD，默认 UPLOAD */
    private String sourceType;

    public Long getSourceAssetId() {
        return sourceAssetId;
    }

    public void setSourceAssetId(Long sourceAssetId) {
        this.sourceAssetId = sourceAssetId;
    }

    public String getScenarioCode() {
        return scenarioCode;
    }

    public void setScenarioCode(String scenarioCode) {
        this.scenarioCode = scenarioCode;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
}
