package com.zhs.service;


import com.zhs.model.FileAsset;
import com.zhs.dto.FileAssetDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-08-28
 */

public interface IFileAssetService extends IService<FileAsset> {
    R addOrUpdate(FileAssetDto fileAssetDto);
}
