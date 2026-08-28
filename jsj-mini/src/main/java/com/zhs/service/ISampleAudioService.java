package com.zhs.service;


import com.zhs.model.SampleAudio;
import com.zhs.dto.SampleAudioDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-08-28
 */

public interface ISampleAudioService extends IService<SampleAudio> {
    R addOrUpdate(SampleAudioDto sampleAudioDto);
}
