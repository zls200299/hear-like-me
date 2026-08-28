package com.zhs.dao;


import com.zhs.model.FileAsset;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 统一文件资源表 的dao
 *
 * @author 
 * @since 2026-08-28
 */
@Mapper
@Repository
public interface FileAssetDao extends BaseMapper<FileAsset> {

}
