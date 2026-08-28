package com.zhs.service.impl;


import com.zhs.model.AudioProcessingTask;
import com.zhs.dao.AudioProcessingTaskDao;
import com.zhs.service.IAudioProcessingTaskService;
import com.zhs.dto.AudioProcessingTaskDto;


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
public class AudioProcessingTaskServiceImpl extends ServiceImpl< AudioProcessingTaskDao, AudioProcessingTask> implements IAudioProcessingTaskService {

    @Resource
    private AudioProcessingTaskDao audioProcessingTaskDao;

    @Override
    public R addOrUpdate(AudioProcessingTaskDto audioProcessingTaskDto) {
        if (BeanUtil.isEmpty(audioProcessingTaskDto)) throw new ServiceException("数据不能为空");
        AudioProcessingTask audioProcessingTask = new AudioProcessingTask();
        BeanUtil.copyProperties(audioProcessingTaskDto,audioProcessingTask);
        if (audioProcessingTaskDto.getId() == null){
            audioProcessingTaskDao.insert(audioProcessingTask);
            return R.ok("数据插入成功");
        }else {
            audioProcessingTaskDao.updateById(audioProcessingTask);
            return R.ok("数据更新成功");
        }
    }
}