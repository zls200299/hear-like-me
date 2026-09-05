package com.zhs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.zhs.exception.ServiceException;
import com.zhs.model.AudioProcessingTask;
import com.zhs.model.AudioProcessingTaskEvent;
import com.zhs.service.IAudioProcessingTaskEventService;
import com.zhs.service.IAudioProcessingTaskService;
import com.zhs.util.PageQueryUtil;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 音频处理任务（运维）
 */
@RestController
@RequestMapping("/audio/processing/task")
@Api(value = "音频处理任务")
@Slf4j
public class AudioProcessingTaskController {

    @Resource
    private IAudioProcessingTaskService iAudioProcessingTaskService;

    @Resource
    private IAudioProcessingTaskEventService iAudioProcessingTaskEventService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<Map<String, Object>> getListByPage(
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(value = "taskNo", required = false) String taskNo,
            @RequestParam(value = "taskStatus", required = false) String taskStatus,
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "scenarioCode", required = false) String scenarioCode) {

        LambdaQueryWrapper<AudioProcessingTask> lambda = new QueryWrapper<AudioProcessingTask>().lambda()
                .eq(AudioProcessingTask::getIsDelete, 0);

        if (StringUtils.isNotBlank(taskNo)) {
            lambda.like(AudioProcessingTask::getTaskNo, taskNo.trim());
        }
        if (StringUtils.isNotBlank(taskStatus)) {
            lambda.eq(AudioProcessingTask::getTaskStatus, taskStatus.trim());
        }
        if (StringUtils.isNotBlank(sourceType)) {
            lambda.eq(AudioProcessingTask::getSourceType, sourceType.trim());
        }
        if (StringUtils.isNotBlank(userId)) {
            lambda.eq(AudioProcessingTask::getUserId, userId.trim());
        }
        if (StringUtils.isNotBlank(scenarioCode)) {
            lambda.like(AudioProcessingTask::getScenarioCode, scenarioCode.trim());
        }

        lambda.orderByDesc(AudioProcessingTask::getCreateTime)
                .orderByDesc(AudioProcessingTask::getId);

        return R.ok(PageQueryUtil.queryPage(iAudioProcessingTaskService, currentPage, pageSize, lambda));
    }

    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<AudioProcessingTask> getById(@RequestParam("id") String id) {
        if (StringUtils.isBlank(id)) {
            throw new ServiceException("id不能为空");
        }
        LambdaQueryWrapper<AudioProcessingTask> wrapper = new QueryWrapper<AudioProcessingTask>().lambda()
                .eq(AudioProcessingTask::getId, id)
                .eq(AudioProcessingTask::getIsDelete, 0);
        return R.ok(iAudioProcessingTaskService.getOne(wrapper));
    }

    @ApiOperation(value = "任务详情（含事件日志）")
    @GetMapping("/detail")
    public R<Map<String, Object>> detail(@RequestParam("id") String id) {
        if (StringUtils.isBlank(id)) {
            throw new ServiceException("id不能为空");
        }
        AudioProcessingTask task = iAudioProcessingTaskService.getOne(new QueryWrapper<AudioProcessingTask>().lambda()
                .eq(AudioProcessingTask::getId, id)
                .eq(AudioProcessingTask::getIsDelete, 0));
        if (ObjectUtils.isEmpty(task)) {
            throw new ServiceException("任务不存在或已删除");
        }

        List<AudioProcessingTaskEvent> events = iAudioProcessingTaskEventService.list(
                new QueryWrapper<AudioProcessingTaskEvent>().lambda()
                        .eq(AudioProcessingTaskEvent::getTaskId, task.getId())
                        .orderByAsc(AudioProcessingTaskEvent::getCreateTime)
                        .orderByAsc(AudioProcessingTaskEvent::getId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task", task);
        result.put("events", events);
        return R.ok(result);
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R<String> deleteById(@PathVariable("id") String id) {
        if (StringUtils.isBlank(id)) {
            throw new ServiceException("id不能为空");
        }
        LambdaQueryWrapper<AudioProcessingTask> query = new QueryWrapper<AudioProcessingTask>().lambda()
                .eq(AudioProcessingTask::getId, id)
                .eq(AudioProcessingTask::getIsDelete, 0);
        AudioProcessingTask audioProcessingTask = iAudioProcessingTaskService.getOne(query);
        if (ObjectUtils.isEmpty(audioProcessingTask)) {
            throw new ServiceException("该数据不存在或者已经被删除");
        }
        audioProcessingTask.setIsDelete(1);
        iAudioProcessingTaskService.updateById(audioProcessingTask);
        return R.ok("数据删除成功");
    }
}
