package com.zhs.controller;



import com.zhs.service.IContentArticleService;
import com.zhs.model.ContentArticle;
import com.zhs.dto.ContentArticleDto;

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
@RequestMapping("/content/article")
@Api(value = "")
@Slf4j
public class ContentArticleController {

    @Resource
    private IContentArticleService  iContentArticleService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<ContentArticle>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<ContentArticle> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<ContentArticle> lambda = new QueryWrapper<ContentArticle>().lambda();
        //此处可以拼条件
        lambda.eq(ContentArticle::getIsDelete,0);
        IPage<ContentArticle> pages =  iContentArticleService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<ContentArticle> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<ContentArticle> wrapper = new QueryWrapper<ContentArticle>()
                            .lambda().eq(ContentArticle::getId,id).eq(ContentArticle::getIsDelete,0);
        return R.ok(iContentArticleService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<ContentArticle> query = new QueryWrapper<ContentArticle>().lambda().eq(ContentArticle::getId, id).eq(ContentArticle::getIsDelete, 0);
        ContentArticle contentArticle = iContentArticleService.getOne(query);
        if(ObjectUtils.isEmpty(contentArticle)) throw new ServiceException("该数据不存在或者已经被删除");
        contentArticle.setIsDelete(1);
        iContentArticleService.updateById(contentArticle);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody ContentArticleDto contentArticleDto){
        if (CollectionUtils.isEmpty(contentArticleDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<ContentArticle> list = new ArrayList<>();
        contentArticleDto.getIdList().stream().forEach(id ->{
        ContentArticle contentArticle = iContentArticleService.getById(id);
            if (ObjectUtils.isEmpty(contentArticle)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == contentArticle.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            contentArticle.setIsDelete(1);
            list.add(contentArticle);
        });
        iContentArticleService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody ContentArticleDto contentArticleDto){
        return iContentArticleService.addOrUpdate(contentArticleDto);
    }

}
