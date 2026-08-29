package com.zhs.service.engine;

import lombok.Data;

import java.nio.file.Path;
import java.util.Map;

@Data
public class VocoderResult {

    private Path outputPath;

    private Integer clarityScore;

    private String clarityGrade;

    private Map<String, Object> visualizationData;
}
