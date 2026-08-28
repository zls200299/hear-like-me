package com.zhs.controller;



import com.zhs.service.IContentCategoryService;
import com.zhs.model.ContentCategory;
import com.zhs.dto.ContentCategoryDto;

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
@RequestMapping("/content/category")
@Api(value = "")
@Slf4j
public class ContentCategoryController {

    @Resource
    private IContentCategoryService  iContentCategoryService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<ContentCategory>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<ContentCategory> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<ContentCategory> lambda = new QueryWrapper<ContentCategory>().lambda();
        //此处可以拼条件
        lambda.eq(ContentCategory::getIsDelete,0);
        IPage<ContentCategory> pages =  iContentCategoryService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<ContentCategory> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<ContentCategory> wrapper = new QueryWrapper<ContentCategory>()
                            .lambda().eq(ContentCategory::getId,id).eq(ContentCategory::getIsDelete,0);
        return R.ok(iContentCategoryService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<ContentCategory> query = new QueryWrapper<ContentCategory>().lambda().eq(ContentCategory::getId, id).eq(ContentCategory::getIsDelete, 0);
        ContentCategory contentCategory = iContentCategoryService.getOne(query);
        if(ObjectUtils.isEmpty(contentCategory)) throw new ServiceException("该数据不存在或者已经被删除");
        contentCategory.setIsDelete(1);
        iContentCategoryService.updateById(contentCategory);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody ContentCategoryDto contentCategoryDto){
        if (CollectionUtils.isEmpty(contentCategoryDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<ContentCategory> list = new ArrayList<>();
        contentCategoryDto.getIdList().stream().forEach(id ->{
        ContentCategory contentCategory = iContentCategoryService.getById(id);
            if (ObjectUtils.isEmpty(contentCategory)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == contentCategory.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            contentCategory.setIsDelete(1);
            list.add(contentCategory);
        });
        iContentCategoryService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody ContentCategoryDto contentCategoryDto){
        return iContentCategoryService.addOrUpdate(contentCategoryDto);
    }

}
