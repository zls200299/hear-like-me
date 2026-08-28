package com.zhs.service;


import com.zhs.model.SystemConfig;
import com.zhs.dto.SystemConfigDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-08-28
 */

public interface ISystemConfigService extends IService<SystemConfig> {
    R addOrUpdate(SystemConfigDto systemConfigDto);
}
