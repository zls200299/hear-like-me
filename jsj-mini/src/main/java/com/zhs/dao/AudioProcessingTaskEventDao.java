package com.zhs.dao;


import com.zhs.model.AudioProcessingTaskEvent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 音频处理任务事件日志 的dao
 *
 * @author 
 * @since 2026-08-28
 */
@Mapper
@Repository
public interface AudioProcessingTaskEventDao extends BaseMapper<AudioProcessingTaskEvent> {

}
