package com.zhs.service;


import com.zhs.model.UserLoginSession;
import com.zhs.dto.UserLoginSessionDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-08-28
 */

public interface IUserLoginSessionService extends IService<UserLoginSession> {
    R addOrUpdate(UserLoginSessionDto userLoginSessionDto);
}
