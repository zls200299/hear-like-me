package com.zhs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.dto.ChallengeAudioDto;
import com.zhs.model.ChallengeAudio;
import com.zhs.util.R;

public interface ChallengeAudioService extends IService<ChallengeAudio> {

    R<ChallengeAudio> addOrUpdate(ChallengeAudioDto dto);

    R<ChallengeAudio> generate(Long id);

    R<String> deleteAudio(Long id);
}
