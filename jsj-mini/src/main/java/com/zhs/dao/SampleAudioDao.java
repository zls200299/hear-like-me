package com.zhs.dao;


import com.zhs.model.SampleAudio;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 内置示例音 的dao
 *
 * @author 
 * @since 2026-08-28
 */
@Mapper
@Repository
public interface SampleAudioDao extends BaseMapper<SampleAudio> {

}
