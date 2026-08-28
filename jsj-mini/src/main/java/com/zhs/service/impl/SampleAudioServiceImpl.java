package com.zhs.service.impl;


import com.zhs.model.SampleAudio;
import com.zhs.dao.SampleAudioDao;
import com.zhs.service.ISampleAudioService;
import com.zhs.dto.SampleAudioDto;


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
public class SampleAudioServiceImpl extends ServiceImpl< SampleAudioDao, SampleAudio> implements ISampleAudioService {

    @Resource
    private SampleAudioDao sampleAudioDao;

    @Override
    public R addOrUpdate(SampleAudioDto sampleAudioDto) {
        if (BeanUtil.isEmpty(sampleAudioDto)) throw new ServiceException("数据不能为空");
        SampleAudio sampleAudio = new SampleAudio();
        BeanUtil.copyProperties(sampleAudioDto,sampleAudio);
        if (sampleAudioDto.getId() == null){
            sampleAudioDao.insert(sampleAudio);
            return R.ok("数据插入成功");
        }else {
            sampleAudioDao.updateById(sampleAudio);
            return R.ok("数据更新成功");
        }
    }
}