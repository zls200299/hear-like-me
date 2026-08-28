package com.zhs.service.impl;


import com.zhs.model.SystemConfig;
import com.zhs.dao.SystemConfigDao;
import com.zhs.service.ISystemConfigService;
import com.zhs.dto.SystemConfigDto;


import com.zhs.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.Date;


/**
 *
 * @author 
 * @since 2026-08-28
 */
@Service
@Slf4j
public class SystemConfigServiceImpl extends ServiceImpl< SystemConfigDao, SystemConfig> implements ISystemConfigService {

    @Resource
    private SystemConfigDao systemConfigDao;

    @Override
    public R addOrUpdate(SystemConfigDto systemConfigDto) {
        if (BeanUtil.isEmpty(systemConfigDto)) throw new ServiceException("数据不能为空");
        SystemConfig systemConfig = new SystemConfig();
        BeanUtil.copyProperties(systemConfigDto,systemConfig);
        if (systemConfigDto.getId() == null){
            systemConfigDao.insert(systemConfig);
            return R.ok("数据插入成功");
        }else {
            systemConfigDao.updateById(systemConfig);
            return R.ok("数据更新成功");
        }
    }
}