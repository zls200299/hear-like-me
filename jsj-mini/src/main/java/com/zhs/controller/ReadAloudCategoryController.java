package com.zhs.controller;



import com.zhs.service.IReadAloudCategoryService;
import com.zhs.model.ReadAloudCategory;
import com.zhs.dto.ReadAloudCategoryDto;

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
@RequestMapping("/read/aloud/category")
@Api(value = "")
@Slf4j
public class ReadAloudCategoryController {

    @Resource
    private IReadAloudCategoryService  iReadAloudCategoryService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<ReadAloudCategory>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "enabled", required = false) Integer enabled){
        Page<ReadAloudCategory> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<ReadAloudCategory> lambda = new QueryWrapper<ReadAloudCategory>().lambda()
                .eq(ReadAloudCategory::getIsDelete, 0);
        if (StringUtils.isNotBlank(keyword)) {
            lambda.and(w -> w.like(ReadAloudCategory::getNameCn, keyword)
                    .or().like(ReadAloudCategory::getCategoryCode, keyword));
        }
        if (enabled != null) {
            lambda.eq(ReadAloudCategory::getEnabled, enabled);
        }
        lambda.orderByAsc(ReadAloudCategory::getSortOrder).orderByAsc(ReadAloudCategory::getId);
        IPage<ReadAloudCategory> pages = iReadAloudCategoryService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<ReadAloudCategory> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<ReadAloudCategory> wrapper = new QueryWrapper<ReadAloudCategory>()
                            .lambda().eq(ReadAloudCategory::getId,id).eq(ReadAloudCategory::getIsDelete,0);
        return R.ok(iReadAloudCategoryService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        return iReadAloudCategoryService.deleteByIdSafe(id);
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody ReadAloudCategoryDto readAloudCategoryDto){
        if (CollectionUtils.isEmpty(readAloudCategoryDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<ReadAloudCategory> list = new ArrayList<>();
        readAloudCategoryDto.getIdList().stream().forEach(id ->{
        ReadAloudCategory readAloudCategory = iReadAloudCategoryService.getById(id);
            if (ObjectUtils.isEmpty(readAloudCategory)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == readAloudCategory.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            readAloudCategory.setIsDelete(1);
            list.add(readAloudCategory);
        });
        iReadAloudCategoryService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody ReadAloudCategoryDto readAloudCategoryDto){
        return iReadAloudCategoryService.addOrUpdate(readAloudCategoryDto);
    }

}
