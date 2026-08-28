package com.zhs.dao;


import com.zhs.model.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


/**
 * 小程序/网站统一用户主表 的dao
 *
 * @author 
 * @since 2026-08-28
 */
@Mapper
@Repository
public interface UserDao extends BaseMapper<User> {

}
