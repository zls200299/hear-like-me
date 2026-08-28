package com.zhs.service.impl;


import com.zhs.model.ContentArticle;
import com.zhs.dao.ContentArticleDao;
import com.zhs.service.IContentArticleService;
import com.zhs.dto.ContentArticleDto;


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
public class ContentArticleServiceImpl extends ServiceImpl< ContentArticleDao, ContentArticle> implements IContentArticleService {

    @Resource
    private ContentArticleDao contentArticleDao;

    @Override
    public R addOrUpdate(ContentArticleDto contentArticleDto) {
        if (BeanUtil.isEmpty(contentArticleDto)) throw new ServiceException("数据不能为空");
        ContentArticle contentArticle = new ContentArticle();
        BeanUtil.copyProperties(contentArticleDto,contentArticle);
        if (contentArticleDto.getId() == null){
            contentArticleDao.insert(contentArticle);
            return R.ok("数据插入成功");
        }else {
            contentArticleDao.updateById(contentArticle);
            return R.ok("数据更新成功");
        }
    }
}