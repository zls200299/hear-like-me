package com.zhs.controller;



import com.zhs.service.IUserLoginSessionService;
import com.zhs.model.UserLoginSession;
import com.zhs.dto.UserLoginSessionDto;

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
@RequestMapping("/user/login/session")
@Api(value = "")
@Slf4j
public class UserLoginSessionController {

    @Resource
    private IUserLoginSessionService  iUserLoginSessionService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<UserLoginSession>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<UserLoginSession> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<UserLoginSession> lambda = new QueryWrapper<UserLoginSession>().lambda();
        //此处可以拼条件
        lambda.eq(UserLoginSession::getIsDelete,0);
        IPage<UserLoginSession> pages =  iUserLoginSessionService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<UserLoginSession> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<UserLoginSession> wrapper = new QueryWrapper<UserLoginSession>()
                            .lambda().eq(UserLoginSession::getId,id).eq(UserLoginSession::getIsDelete,0);
        return R.ok(iUserLoginSessionService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<UserLoginSession> query = new QueryWrapper<UserLoginSession>().lambda().eq(UserLoginSession::getId, id).eq(UserLoginSession::getIsDelete, 0);
        UserLoginSession userLoginSession = iUserLoginSessionService.getOne(query);
        if(ObjectUtils.isEmpty(userLoginSession)) throw new ServiceException("该数据不存在或者已经被删除");
        userLoginSession.setIsDelete(1);
        iUserLoginSessionService.updateById(userLoginSession);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody UserLoginSessionDto userLoginSessionDto){
        if (CollectionUtils.isEmpty(userLoginSessionDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<UserLoginSession> list = new ArrayList<>();
        userLoginSessionDto.getIdList().stream().forEach(id ->{
        UserLoginSession userLoginSession = iUserLoginSessionService.getById(id);
            if (ObjectUtils.isEmpty(userLoginSession)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == userLoginSession.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            userLoginSession.setIsDelete(1);
            list.add(userLoginSession);
        });
        iUserLoginSessionService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody UserLoginSessionDto userLoginSessionDto){
        return iUserLoginSessionService.addOrUpdate(userLoginSessionDto);
    }

}
