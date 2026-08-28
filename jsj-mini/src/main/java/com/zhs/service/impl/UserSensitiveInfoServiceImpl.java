package com.zhs.service.impl;


import com.zhs.model.UserSensitiveInfo;
import com.zhs.dao.UserSensitiveInfoDao;
import com.zhs.service.IUserSensitiveInfoService;
import com.zhs.dto.UserSensitiveInfoDto;


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
public class UserSensitiveInfoServiceImpl extends ServiceImpl< UserSensitiveInfoDao, UserSensitiveInfo> implements IUserSensitiveInfoService {

    @Resource
    private UserSensitiveInfoDao userSensitiveInfoDao;

    @Override
    public R addOrUpdate(UserSensitiveInfoDto userSensitiveInfoDto) {
        if (BeanUtil.isEmpty(userSensitiveInfoDto)) throw new ServiceException("数据不能为空");
        UserSensitiveInfo userSensitiveInfo = new UserSensitiveInfo();
        BeanUtil.copyProperties(userSensitiveInfoDto,userSensitiveInfo);
        if (userSensitiveInfoDto.getId() == null){
            userSensitiveInfoDao.insert(userSensitiveInfo);
            return R.ok("数据插入成功");
        }else {
            userSensitiveInfoDao.updateById(userSensitiveInfo);
            return R.ok("数据更新成功");
        }
    }
}