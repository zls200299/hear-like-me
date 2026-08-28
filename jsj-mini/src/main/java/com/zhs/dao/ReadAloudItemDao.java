package com.zhs.dao;


import com.zhs.model.ReadAloudItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 图片点读内容 的dao
 *
 * @author 
 * @since 2026-08-28
 */
@Mapper
@Repository
public interface ReadAloudItemDao extends BaseMapper<ReadAloudItem> {

}
