package com.zhs.service.impl;


import com.zhs.model.ContentCategory;
import com.zhs.dao.ContentCategoryDao;
import com.zhs.service.IContentCategoryService;
import com.zhs.dto.ContentCategoryDto;


import com.zhs.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.Date;


/**
 *
 * @author 
 * @since 2026-08-28
 */
@Service
@Slf4j
public class ContentCategoryServiceImpl extends ServiceImpl< ContentCategoryDao, ContentCategory> implements IContentCategoryService {

    @Resource
    private ContentCategoryDao contentCategoryDao;

    @Override
    public R addOrUpdate(ContentCategoryDto contentCategoryDto) {
        if (BeanUtil.isEmpty(contentCategoryDto)) throw new ServiceException("数据不能为空");
        ContentCategory contentCategory = new ContentCategory();
        BeanUtil.copyProperties(contentCategoryDto,contentCategory);
        if (contentCategoryDto.getId() == null){
            contentCategoryDao.insert(contentCategory);
            return R.ok("数据插入成功");
        }else {
            contentCategoryDao.updateById(contentCategory);
            return R.ok("数据更新成功");
        }
    }
}