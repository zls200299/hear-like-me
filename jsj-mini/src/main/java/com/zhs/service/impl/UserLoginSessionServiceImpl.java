package com.zhs.service.impl;


import com.zhs.model.UserLoginSession;
import com.zhs.dao.UserLoginSessionDao;
import com.zhs.service.IUserLoginSessionService;
import com.zhs.dto.UserLoginSessionDto;


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
public class UserLoginSessionServiceImpl extends ServiceImpl< UserLoginSessionDao, UserLoginSession> implements IUserLoginSessionService {

    @Resource
    private UserLoginSessionDao userLoginSessionDao;

    @Override
    public R addOrUpdate(UserLoginSessionDto userLoginSessionDto) {
        if (BeanUtil.isEmpty(userLoginSessionDto)) throw new ServiceException("数据不能为空");
        UserLoginSession userLoginSession = new UserLoginSession();
        BeanUtil.copyProperties(userLoginSessionDto,userLoginSession);
        if (userLoginSessionDto.getId() == null){
            userLoginSessionDao.insert(userLoginSession);
            return R.ok("数据插入成功");
        }else {
            userLoginSessionDao.updateById(userLoginSession);
            return R.ok("数据更新成功");
        }
    }
}