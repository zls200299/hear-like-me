package com.zhs.dao;


import com.zhs.model.SystemConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * Hear Like Me 业务系统配置 的dao
 *
 * @author 
 * @since 2026-08-28
 */
@Mapper
@Repository
public interface SystemConfigDao extends BaseMapper<SystemConfig> {

}
