package com.zhs.dao;


import com.zhs.model.AudioProcessingTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 音频处理任务/历史记录 的dao
 *
 * @author 
 * @since 2026-08-28
 */
@Mapper
@Repository
public interface AudioProcessingTaskDao extends BaseMapper<AudioProcessingTask> {

}
