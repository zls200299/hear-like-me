package com.zhs.dao;


import com.zhs.model.ContentArticle;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 科普文章/页面 的dao
 *
 * @author 
 * @since 2026-08-28
 */
@Mapper
@Repository
public interface ContentArticleDao extends BaseMapper<ContentArticle> {

}
