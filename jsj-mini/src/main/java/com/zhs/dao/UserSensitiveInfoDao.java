package com.zhs.dao;


import com.zhs.model.UserSensitiveInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 用户微信敏感身份信息 的dao
 *
 * @author 
 * @since 2026-08-28
 */
@Mapper
@Repository
public interface UserSensitiveInfoDao extends BaseMapper<UserSensitiveInfo> {

}
