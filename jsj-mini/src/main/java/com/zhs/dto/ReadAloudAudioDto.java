package com.zhs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ReadAloudAudioDto implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String audioCode;
    private String title;
    private String description;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceAssetId;

    @JsonProperty("nChannels")
    private Integer nChannels;

    private String carrier;

    @JsonProperty("fLo")
    private BigDecimal fLo;

    @JsonProperty("fHi")
    private BigDecimal fHi;

    private BigDecimal envCut;
    private BigDecimal spread;
    private BigDecimal noiseLevel;
    private String status;
}
