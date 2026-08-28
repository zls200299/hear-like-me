package com.zhs.dao;


import com.zhs.model.ReadAloudCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 图片点读分类 的dao
 *
 * @author 
 * @since 2026-08-28
 */
@Mapper
@Repository
public interface ReadAloudCategoryDao extends BaseMapper<ReadAloudCategory> {

}
