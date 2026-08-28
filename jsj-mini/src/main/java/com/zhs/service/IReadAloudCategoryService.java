package com.zhs.service;


import com.zhs.model.ReadAloudCategory;
import com.zhs.dto.ReadAloudCategoryDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-08-28
 */

public interface IReadAloudCategoryService extends IService<ReadAloudCategory> {
    R addOrUpdate(ReadAloudCategoryDto readAloudCategoryDto);
}
