package com.zhs.service.engine;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VocoderParams {

    private int nChannels = 8;

    private String carrier = "noise";

    private BigDecimal fLo = BigDecimal.valueOf(150);

    private BigDecimal fHi = BigDecimal.valueOf(7000);

    private BigDecimal envCut = BigDecimal.valueOf(160);

    /** 0-1 */
    private BigDecimal spread = BigDecimal.ZERO;

    /** 0-1 */
    private BigDecimal noiseLevel = BigDecimal.ZERO;

    private String scenarioCode;
}
