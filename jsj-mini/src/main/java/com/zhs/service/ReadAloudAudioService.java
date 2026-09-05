package com.zhs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.dto.ReadAloudAudioDto;
import com.zhs.model.ReadAloudAudio;
import com.zhs.util.R;

public interface ReadAloudAudioService extends IService<ReadAloudAudio> {

    R<ReadAloudAudio> addOrUpdate(ReadAloudAudioDto dto);

    R<ReadAloudAudio> generate(Long id);

    R<String> deleteAudio(Long id);
}
