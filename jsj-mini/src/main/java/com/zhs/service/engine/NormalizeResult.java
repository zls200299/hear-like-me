package com.zhs.service.engine;

import com.zhs.model.FileAsset;
import lombok.Data;

import java.nio.file.Path;

@Data
public class NormalizeResult {

    private FileAsset asset;

    private Path localPath;
}
