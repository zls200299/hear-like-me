package com.zhs.service;


import com.zhs.model.User;
import com.zhs.dto.UserDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-08-28
 */

public interface IUserService extends IService<User> {
    R addOrUpdate(UserDto userDto);
}
