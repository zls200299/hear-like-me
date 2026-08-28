package com.zhs.dao;


import com.zhs.model.ScenarioPreset;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 人工耳蜗场景预设 的dao
 *
 * @author 
 * @since 2026-08-28
 */
@Mapper
@Repository
public interface ScenarioPresetDao extends BaseMapper<ScenarioPreset> {

}
