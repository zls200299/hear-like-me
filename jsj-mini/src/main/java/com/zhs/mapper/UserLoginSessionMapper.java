package com.zhs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.model.UserLoginSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserLoginSessionMapper extends BaseMapper<UserLoginSession> {
}
