package com.zhs.controller;



import com.zhs.service.IUserService;
import com.zhs.model.User;
import com.zhs.dto.UserDto;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.zhs.exception.ServiceException;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author 
 * @since 2026-08-28
 */
@RestController
@RequestMapping("/user")
@Api(value = "")
@Slf4j
public class UserController {

    @Resource
    private IUserService  iUserService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<User>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<User> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<User> lambda = new QueryWrapper<User>().lambda();
        //此处可以拼条件
        lambda.eq(User::getIsDelete,0);
        IPage<User> pages =  iUserService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<User> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<User> wrapper = new QueryWrapper<User>()
                            .lambda().eq(User::getId,id).eq(User::getIsDelete,0);
        return R.ok(iUserService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<User> query = new QueryWrapper<User>().lambda().eq(User::getId, id).eq(User::getIsDelete, 0);
        User user = iUserService.getOne(query);
        if(ObjectUtils.isEmpty(user)) throw new ServiceException("该数据不存在或者已经被删除");
        user.setIsDelete(1);
        iUserService.updateById(user);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody UserDto userDto){
        if (CollectionUtils.isEmpty(userDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<User> list = new ArrayList<>();
        userDto.getIdList().stream().forEach(id ->{
        User user = iUserService.getById(id);
            if (ObjectUtils.isEmpty(user)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == user.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            user.setIsDelete(1);
            list.add(user);
        });
        iUserService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody UserDto userDto){
        return iUserService.addOrUpdate(userDto);
    }

}
