package com.zhs.controller;



import com.zhs.service.IAudioProcessingTaskService;
import com.zhs.model.AudioProcessingTask;
import com.zhs.dto.AudioProcessingTaskDto;

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
@RequestMapping("/audio/processing/task")
@Api(value = "")
@Slf4j
public class AudioProcessingTaskController {

    @Resource
    private IAudioProcessingTaskService  iAudioProcessingTaskService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<AudioProcessingTask>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<AudioProcessingTask> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<AudioProcessingTask> lambda = new QueryWrapper<AudioProcessingTask>().lambda();
        //此处可以拼条件
        lambda.eq(AudioProcessingTask::getIsDelete,0);
        IPage<AudioProcessingTask> pages =  iAudioProcessingTaskService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<AudioProcessingTask> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<AudioProcessingTask> wrapper = new QueryWrapper<AudioProcessingTask>()
                            .lambda().eq(AudioProcessingTask::getId,id).eq(AudioProcessingTask::getIsDelete,0);
        return R.ok(iAudioProcessingTaskService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<AudioProcessingTask> query = new QueryWrapper<AudioProcessingTask>().lambda().eq(AudioProcessingTask::getId, id).eq(AudioProcessingTask::getIsDelete, 0);
        AudioProcessingTask audioProcessingTask = iAudioProcessingTaskService.getOne(query);
        if(ObjectUtils.isEmpty(audioProcessingTask)) throw new ServiceException("该数据不存在或者已经被删除");
        audioProcessingTask.setIsDelete(1);
        iAudioProcessingTaskService.updateById(audioProcessingTask);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody AudioProcessingTaskDto audioProcessingTaskDto){
        if (CollectionUtils.isEmpty(audioProcessingTaskDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<AudioProcessingTask> list = new ArrayList<>();
        audioProcessingTaskDto.getIdList().stream().forEach(id ->{
        AudioProcessingTask audioProcessingTask = iAudioProcessingTaskService.getById(id);
            if (ObjectUtils.isEmpty(audioProcessingTask)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == audioProcessingTask.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            audioProcessingTask.setIsDelete(1);
            list.add(audioProcessingTask);
        });
        iAudioProcessingTaskService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody AudioProcessingTaskDto audioProcessingTaskDto){
        return iAudioProcessingTaskService.addOrUpdate(audioProcessingTaskDto);
    }

}
