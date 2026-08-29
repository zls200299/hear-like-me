package com.zhs.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 创建音频处理任务请求（小程序 API）
 */
public class AudioTaskCreateReq implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceAssetId;

    /** SAMPLE / UPLOAD / RECORDING / READ_ALOUD，默认 UPLOAD */
    private String sourceType;

    private String scenarioCode;

    private Integer nChannels;

    private String carrier;

    private BigDecimal fLo;

    private BigDecimal fHi;

    private BigDecimal envCut;

    private BigDecimal spread;

    private BigDecimal noiseLevel;

    public Long getSourceAssetId() {
        return sourceAssetId;
    }

    public void setSourceAssetId(Long sourceAssetId) {
        this.sourceAssetId = sourceAssetId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getScenarioCode() {
        return scenarioCode;
    }

    public void setScenarioCode(String scenarioCode) {
        this.scenarioCode = scenarioCode;
    }

    public Integer getNChannels() {
        return nChannels;
    }

    public void setNChannels(Integer nChannels) {
        this.nChannels = nChannels;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public BigDecimal getFLo() {
        return fLo;
    }

    public void setFLo(BigDecimal fLo) {
        this.fLo = fLo;
    }

    public BigDecimal getFHi() {
        return fHi;
    }

    public void setFHi(BigDecimal fHi) {
        this.fHi = fHi;
    }

    public BigDecimal getEnvCut() {
        return envCut;
    }

    public void setEnvCut(BigDecimal envCut) {
        this.envCut = envCut;
    }

    public BigDecimal getSpread() {
        return spread;
    }

    public void setSpread(BigDecimal spread) {
        this.spread = spread;
    }

    public BigDecimal getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(BigDecimal noiseLevel) {
        this.noiseLevel = noiseLevel;
    }
}
