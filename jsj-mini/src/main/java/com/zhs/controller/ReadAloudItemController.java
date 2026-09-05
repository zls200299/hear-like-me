package com.zhs.controller;



import com.zhs.service.IReadAloudItemService;
import com.zhs.model.ReadAloudItem;
import com.zhs.dto.ReadAloudItemDto;

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
@RequestMapping("/read/aloud/item")
@Api(value = "")
@Slf4j
public class ReadAloudItemController {

    @Resource
    private IReadAloudItemService  iReadAloudItemService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<ReadAloudItem>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
        @RequestParam(value = "categoryId", required = false) Long categoryId,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "keyword", required = false) String keyword){
        Page<ReadAloudItem> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<ReadAloudItem> lambda = new QueryWrapper<ReadAloudItem>().lambda()
                .eq(ReadAloudItem::getIsDelete, 0);
        if (categoryId != null) {
            lambda.eq(ReadAloudItem::getCategoryId, categoryId);
        }
        if (StringUtils.isNotBlank(status)) {
            lambda.eq(ReadAloudItem::getStatus, status);
        }
        if (StringUtils.isNotBlank(keyword)) {
            lambda.and(w -> w.like(ReadAloudItem::getTitleCn, keyword)
                    .or().like(ReadAloudItem::getItemCode, keyword));
        }
        lambda.orderByAsc(ReadAloudItem::getSortOrder).orderByAsc(ReadAloudItem::getId);
        IPage<ReadAloudItem> pages = iReadAloudItemService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<ReadAloudItem> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<ReadAloudItem> wrapper = new QueryWrapper<ReadAloudItem>()
                            .lambda().eq(ReadAloudItem::getId,id).eq(ReadAloudItem::getIsDelete,0);
        return R.ok(iReadAloudItemService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<ReadAloudItem> query = new QueryWrapper<ReadAloudItem>().lambda().eq(ReadAloudItem::getId, id).eq(ReadAloudItem::getIsDelete, 0);
        ReadAloudItem readAloudItem = iReadAloudItemService.getOne(query);
        if(ObjectUtils.isEmpty(readAloudItem)) throw new ServiceException("该数据不存在或者已经被删除");
        readAloudItem.setIsDelete(1);
        iReadAloudItemService.updateById(readAloudItem);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody ReadAloudItemDto readAloudItemDto){
        if (CollectionUtils.isEmpty(readAloudItemDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<ReadAloudItem> list = new ArrayList<>();
        readAloudItemDto.getIdList().stream().forEach(id ->{
        ReadAloudItem readAloudItem = iReadAloudItemService.getById(id);
            if (ObjectUtils.isEmpty(readAloudItem)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == readAloudItem.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            readAloudItem.setIsDelete(1);
            list.add(readAloudItem);
        });
        iReadAloudItemService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody ReadAloudItemDto readAloudItemDto){
        return iReadAloudItemService.addOrUpdate(readAloudItemDto);
    }

}
