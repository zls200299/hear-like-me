package com.zhs.service.impl;


import com.zhs.model.AudioProcessingTaskEvent;
import com.zhs.dao.AudioProcessingTaskEventDao;
import com.zhs.service.IAudioProcessingTaskEventService;
import com.zhs.dto.AudioProcessingTaskEventDto;


import com.zhs.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.Date;


/**
 *
 * @author 
 * @since 2026-08-28
 */
@Service
@Slf4j
public class AudioProcessingTaskEventServiceImpl extends ServiceImpl< AudioProcessingTaskEventDao, AudioProcessingTaskEvent> implements IAudioProcessingTaskEventService {

    @Resource
    private AudioProcessingTaskEventDao audioProcessingTaskEventDao;

    @Override
    public R addOrUpdate(AudioProcessingTaskEventDto audioProcessingTaskEventDto) {
        if (BeanUtil.isEmpty(audioProcessingTaskEventDto)) throw new ServiceException("数据不能为空");
        AudioProcessingTaskEvent audioProcessingTaskEvent = new AudioProcessingTaskEvent();
        BeanUtil.copyProperties(audioProcessingTaskEventDto,audioProcessingTaskEvent);
        if (audioProcessingTaskEventDto.getId() == null){
            audioProcessingTaskEventDao.insert(audioProcessingTaskEvent);
            return R.ok("数据插入成功");
        }else {
            audioProcessingTaskEventDao.updateById(audioProcessingTaskEvent);
            return R.ok("数据更新成功");
        }
    }
}