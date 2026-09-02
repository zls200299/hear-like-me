package com.zhs.controller;



import com.zhs.service.ISampleAudioService;
import com.zhs.model.SampleAudio;
import com.zhs.dto.SampleAudioDto;

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
@RequestMapping("/sample/audio")
@Api(value = "")
@Slf4j
public class SampleAudioController {

    @Resource
    private ISampleAudioService  iSampleAudioService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<Map<String, Object>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        LambdaQueryWrapper<SampleAudio> lambda = new QueryWrapper<SampleAudio>().lambda();
        lambda.eq(SampleAudio::getIsDelete,0).orderByAsc(SampleAudio::getSortOrder);
        return R.ok(PageQueryUtil.queryPage(iSampleAudioService, currentPage, pageSize, lambda));
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<SampleAudio> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<SampleAudio> wrapper = new QueryWrapper<SampleAudio>()
                            .lambda().eq(SampleAudio::getId,id).eq(SampleAudio::getIsDelete,0);
        return R.ok(iSampleAudioService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<SampleAudio> query = new QueryWrapper<SampleAudio>().lambda().eq(SampleAudio::getId, id).eq(SampleAudio::getIsDelete, 0);
        SampleAudio sampleAudio = iSampleAudioService.getOne(query);
        if(ObjectUtils.isEmpty(sampleAudio)) throw new ServiceException("该数据不存在或者已经被删除");
        sampleAudio.setIsDelete(1);
        iSampleAudioService.updateById(sampleAudio);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody SampleAudioDto sampleAudioDto){
        if (CollectionUtils.isEmpty(sampleAudioDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<SampleAudio> list = new ArrayList<>();
        sampleAudioDto.getIdList().stream().forEach(id ->{
        SampleAudio sampleAudio = iSampleAudioService.getById(id);
            if (ObjectUtils.isEmpty(sampleAudio)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == sampleAudio.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            sampleAudio.setIsDelete(1);
            list.add(sampleAudio);
        });
        iSampleAudioService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody SampleAudioDto sampleAudioDto){
        return iSampleAudioService.addOrUpdate(sampleAudioDto);
    }

}
