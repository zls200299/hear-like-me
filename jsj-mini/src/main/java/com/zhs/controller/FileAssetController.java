package com.zhs.controller;



import com.zhs.service.IFileAssetService;
import com.zhs.model.FileAsset;
import com.zhs.dto.FileAssetDto;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhs.util.PageQueryUtil;
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
import java.util.Map;

/**
 *
 * @author 
 * @since 2026-08-28
 */
@RestController
@RequestMapping("/file/asset")
@Api(value = "")
@Slf4j
public class FileAssetController {

    @Resource
    private IFileAssetService  iFileAssetService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<Map<String, Object>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
        @RequestParam(value = "assetType", required = false) String assetType,
        @RequestParam(value = "fileExt", required = false) String fileExt,
        @RequestParam(value = "originalFilename", required = false) String originalFilename){
        LambdaQueryWrapper<FileAsset> lambda = new QueryWrapper<FileAsset>().lambda();
        lambda.eq(FileAsset::getIsDelete,0);
        if (StringUtils.isNotBlank(assetType)) {
            lambda.eq(FileAsset::getAssetType, assetType);
        }
        if (StringUtils.isNotBlank(fileExt)) {
            lambda.eq(FileAsset::getFileExt, fileExt);
        }
        if (StringUtils.isNotBlank(originalFilename)) {
            lambda.like(FileAsset::getOriginalFilename, originalFilename);
        }
        lambda.orderByDesc(FileAsset::getCreateTime);
        return R.ok(PageQueryUtil.queryPage(iFileAssetService, currentPage, pageSize, lambda));
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<FileAsset> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<FileAsset> wrapper = new QueryWrapper<FileAsset>()
                            .lambda().eq(FileAsset::getId,id).eq(FileAsset::getIsDelete,0);
        return R.ok(iFileAssetService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<FileAsset> query = new QueryWrapper<FileAsset>().lambda().eq(FileAsset::getId, id).eq(FileAsset::getIsDelete, 0);
        FileAsset fileAsset = iFileAssetService.getOne(query);
        if(ObjectUtils.isEmpty(fileAsset)) throw new ServiceException("该数据不存在或者已经被删除");
        fileAsset.setIsDelete(1);
        iFileAssetService.updateById(fileAsset);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody FileAssetDto fileAssetDto){
        if (CollectionUtils.isEmpty(fileAssetDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<FileAsset> list = new ArrayList<>();
        fileAssetDto.getIdList().stream().forEach(id ->{
        FileAsset fileAsset = iFileAssetService.getById(id);
            if (ObjectUtils.isEmpty(fileAsset)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == fileAsset.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            fileAsset.setIsDelete(1);
            list.add(fileAsset);
        });
        iFileAssetService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody FileAssetDto fileAssetDto){
        return iFileAssetService.addOrUpdate(fileAssetDto);
    }

}
