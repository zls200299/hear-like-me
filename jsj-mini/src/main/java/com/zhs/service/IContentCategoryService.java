package com.zhs.service;


import com.zhs.model.ContentCategory;
import com.zhs.dto.ContentCategoryDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-08-28
 */

public interface IContentCategoryService extends IService<ContentCategory> {
    R addOrUpdate(ContentCategoryDto contentCategoryDto);
}
