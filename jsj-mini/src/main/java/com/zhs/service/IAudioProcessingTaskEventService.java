package com.zhs.service;


import com.zhs.model.AudioProcessingTaskEvent;
import com.zhs.dto.AudioProcessingTaskEventDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-08-28
 */

public interface IAudioProcessingTaskEventService extends IService<AudioProcessingTaskEvent> {
    R addOrUpdate(AudioProcessingTaskEventDto audioProcessingTaskEventDto);
}
