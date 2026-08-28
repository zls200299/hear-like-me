package com.zhs.service;


import com.zhs.model.ContentArticle;
import com.zhs.dto.ContentArticleDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-08-28
 */

public interface IContentArticleService extends IService<ContentArticle> {
    R addOrUpdate(ContentArticleDto contentArticleDto);
}
