package com.zhs.controller;



import com.zhs.service.IUserSensitiveInfoService;
import com.zhs.model.UserSensitiveInfo;
import com.zhs.dto.UserSensitiveInfoDto;

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
@RequestMapping("/user/sensitive/info")
@Api(value = "")
@Slf4j
public class UserSensitiveInfoController {

    @Resource
    private IUserSensitiveInfoService  iUserSensitiveInfoService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<UserSensitiveInfo>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<UserSensitiveInfo> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<UserSensitiveInfo> lambda = new QueryWrapper<UserSensitiveInfo>().lambda();
        //此处可以拼条件
        lambda.eq(UserSensitiveInfo::getIsDelete,0);
        IPage<UserSensitiveInfo> pages =  iUserSensitiveInfoService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<UserSensitiveInfo> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<UserSensitiveInfo> wrapper = new QueryWrapper<UserSensitiveInfo>()
                            .lambda().eq(UserSensitiveInfo::getId,id).eq(UserSensitiveInfo::getIsDelete,0);
        return R.ok(iUserSensitiveInfoService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<UserSensitiveInfo> query = new QueryWrapper<UserSensitiveInfo>().lambda().eq(UserSensitiveInfo::getId, id).eq(UserSensitiveInfo::getIsDelete, 0);
        UserSensitiveInfo userSensitiveInfo = iUserSensitiveInfoService.getOne(query);
        if(ObjectUtils.isEmpty(userSensitiveInfo)) throw new ServiceException("该数据不存在或者已经被删除");
        userSensitiveInfo.setIsDelete(1);
        iUserSensitiveInfoService.updateById(userSensitiveInfo);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody UserSensitiveInfoDto userSensitiveInfoDto){
        if (CollectionUtils.isEmpty(userSensitiveInfoDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<UserSensitiveInfo> list = new ArrayList<>();
        userSensitiveInfoDto.getIdList().stream().forEach(id ->{
        UserSensitiveInfo userSensitiveInfo = iUserSensitiveInfoService.getById(id);
            if (ObjectUtils.isEmpty(userSensitiveInfo)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == userSensitiveInfo.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            userSensitiveInfo.setIsDelete(1);
            list.add(userSensitiveInfo);
        });
        iUserSensitiveInfoService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody UserSensitiveInfoDto userSensitiveInfoDto){
        return iUserSensitiveInfoService.addOrUpdate(userSensitiveInfoDto);
    }

}
