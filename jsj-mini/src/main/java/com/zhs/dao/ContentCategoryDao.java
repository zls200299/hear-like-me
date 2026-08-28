package com.zhs.dao;


import com.zhs.model.ContentCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 科普内容分类 的dao
 *
 * @author 
 * @since 2026-08-28
 */
@Mapper
@Repository
public interface ContentCategoryDao extends BaseMapper<ContentCategory> {

}
