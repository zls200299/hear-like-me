package com.zhs.controller;



import com.zhs.service.ISystemConfigService;
import com.zhs.model.SystemConfig;
import com.zhs.dto.SystemConfigDto;

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
@RequestMapping("/system/config")
@Api(value = "")
@Slf4j
public class SystemConfigController {

    @Resource
    private ISystemConfigService  iSystemConfigService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<SystemConfig>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<SystemConfig> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<SystemConfig> lambda = new QueryWrapper<SystemConfig>().lambda();
        // 系统配置表无 is_delete 字段
        IPage<SystemConfig> pages =  iSystemConfigService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<SystemConfig> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<SystemConfig> wrapper = new QueryWrapper<SystemConfig>()
                            .lambda().eq(SystemConfig::getId,id);
        return R.ok(iSystemConfigService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        // 系统配置表无逻辑删除，物理删除
        SystemConfig systemConfig = iSystemConfigService.getById(id);
        if(ObjectUtils.isEmpty(systemConfig)) throw new ServiceException("该数据不存在");
        iSystemConfigService.removeById(id);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody SystemConfigDto systemConfigDto){
        if (CollectionUtils.isEmpty(systemConfigDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        // 系统配置表无逻辑删除，物理删除
        systemConfigDto.getIdList().stream().forEach(id ->{
        SystemConfig systemConfig = iSystemConfigService.getById(id);
            if (ObjectUtils.isEmpty(systemConfig)) throw new ServiceException("id为" + id + "的数据不存在");
        });
        iSystemConfigService.removeByIds(systemConfigDto.getIdList());
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody SystemConfigDto systemConfigDto){
        return iSystemConfigService.addOrUpdate(systemConfigDto);
    }

}
