package com.zhs.service.impl;


import com.zhs.model.User;
import com.zhs.dao.UserDao;
import com.zhs.service.IUserService;
import com.zhs.dto.UserDto;


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
public class UserServiceImpl extends ServiceImpl< UserDao, User> implements IUserService {

    @Resource
    private UserDao userDao;

    @Override
    public R addOrUpdate(UserDto userDto) {
        if (BeanUtil.isEmpty(userDto)) throw new ServiceException("数据不能为空");
        User user = new User();
        BeanUtil.copyProperties(userDto,user);
        if (userDto.getId() == null){
            userDao.insert(user);
            return R.ok("数据插入成功");
        }else {
            userDao.updateById(user);
            return R.ok("数据更新成功");
        }
    }
}