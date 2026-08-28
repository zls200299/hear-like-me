package com.zhs.service;


import com.zhs.model.UserSensitiveInfo;
import com.zhs.dto.UserSensitiveInfoDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-08-28
 */

public interface IUserSensitiveInfoService extends IService<UserSensitiveInfo> {
    R addOrUpdate(UserSensitiveInfoDto userSensitiveInfoDto);
}
