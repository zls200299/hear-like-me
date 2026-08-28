package com.zhs.dao;


import com.zhs.model.UserLoginSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 用户登录会话 的dao
 *
 * @author 
 * @since 2026-08-28
 */
@Mapper
@Repository
public interface UserLoginSessionDao extends BaseMapper<UserLoginSession> {

}
