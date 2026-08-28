package com.zhs.service;


import com.zhs.model.ReadAloudItem;
import com.zhs.dto.ReadAloudItemDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-08-28
 */

public interface IReadAloudItemService extends IService<ReadAloudItem> {
    R addOrUpdate(ReadAloudItemDto readAloudItemDto);
}
