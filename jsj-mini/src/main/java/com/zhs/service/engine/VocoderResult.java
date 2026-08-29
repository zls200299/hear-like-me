package com.zhs.service.engine;

import lombok.Data;

import java.nio.file.Path;

@Data
public class VocoderResult {

    private Path outputPath;

    private Integer clarityScore;

    private String clarityGrade;
}
