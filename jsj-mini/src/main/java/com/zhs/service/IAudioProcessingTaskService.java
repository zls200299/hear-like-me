package com.zhs.service;


import com.zhs.model.AudioProcessingTask;
import com.zhs.dto.AudioProcessingTaskDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-08-28
 */

public interface IAudioProcessingTaskService extends IService<AudioProcessingTask> {
    R addOrUpdate(AudioProcessingTaskDto audioProcessingTaskDto);
}
