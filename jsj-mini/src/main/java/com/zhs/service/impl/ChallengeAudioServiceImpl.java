package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.dao.ChallengeAudioDao;
import com.zhs.dao.HearingChallengeDao;
import com.zhs.dto.ChallengeAudioDto;
import com.zhs.exception.ServiceException;
import com.zhs.model.ChallengeAudio;
import com.zhs.model.FileAsset;
import com.zhs.model.HearingChallenge;
import com.zhs.request.AudioTaskCreateReq;
import com.zhs.service.ChallengeAudioService;
import com.zhs.service.IFileAssetService;
import com.zhs.service.engine.AudioTaskProcessingService;
import com.zhs.util.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ChallengeAudioServiceImpl extends ServiceImpl<ChallengeAudioDao, ChallengeAudio>
        implements ChallengeAudioService {

    private static final Set<Integer> ALLOWED_CHANNELS = Set.of(2, 4, 8, 16);
    private static final Set<String> ALLOWED_CARRIERS = Set.of("noise", "sine");

    @Resource
    private IFileAssetService fileAssetService;

    @Resource
    private AudioTaskProcessingService audioTaskProcessingService;

    @Resource
    private HearingChallengeDao hearingChallengeDao;

    @Override
    @Transactional
    public R<ChallengeAudio> addOrUpdate(ChallengeAudioDto dto) {
        validate(dto);
        ensureCodeUnique(dto.getAudioCode(), dto.getId());

        ChallengeAudio entity;
        boolean generationChanged;
        if (dto.getId() == null) {
            entity = new ChallengeAudio();
            generationChanged = true;
            entity.setVersionNo(0);
            entity.setIsDelete(0);
        } else {
            entity = requireAudio(dto.getId());
            generationChanged = generationChanged(entity, dto);
        }

        entity.setAudioCode(dto.getAudioCode().trim());
        entity.setTitle(dto.getTitle().trim());
        entity.setDescription(dto.getDescription());
        entity.setSourceAssetId(dto.getSourceAssetId());
        entity.setNChannels(dto.getNChannels());
        entity.setCarrier(dto.getCarrier());
        entity.setFLo(dto.getFLo());
        entity.setFHi(dto.getFHi());
        entity.setEnvCut(dto.getEnvCut());
        entity.setSpread(dto.getSpread());
        entity.setNoiseLevel(dto.getNoiseLevel());

        if (generationChanged) {
            entity.setOutputAssetId(null);
            entity.setProcessingTaskNo(null);
            entity.setGeneratedTime(null);
            entity.setErrorMessage(null);
            entity.setStatus("DRAFT");
        } else if ("DISABLED".equals(dto.getStatus())) {
            entity.setStatus("DISABLED");
        } else if ("READY".equals(dto.getStatus()) && entity.getOutputAssetId() != null) {
            entity.setStatus("READY");
        }

        saveOrUpdate(entity);
        return R.ok(entity, dto.getId() == null ? "音频素材已保存，请生成模拟音频" : "保存成功");
    }

    @Override
    public R<ChallengeAudio> generate(Long id) {
        ChallengeAudio entity = requireAudio(id);
        validateEntity(entity);
        entity.setStatus("PROCESSING");
        entity.setErrorMessage(null);
        updateById(entity);

        try {
            AudioTaskCreateReq req = new AudioTaskCreateReq();
            req.setSourceAssetId(entity.getSourceAssetId());
            req.setSourceType("UPLOAD");
            req.setNChannels(entity.getNChannels());
            req.setCarrier(entity.getCarrier());
            req.setFLo(entity.getFLo());
            req.setFHi(entity.getFHi());
            req.setEnvCut(entity.getEnvCut());
            req.setSpread(entity.getSpread());
            req.setNoiseLevel(entity.getNoiseLevel());

            Map<String, Object> result = audioTaskProcessingService.createAndProcess(req);
            entity.setOutputAssetId(Long.valueOf(String.valueOf(result.get("outputAssetId"))));
            entity.setProcessingTaskNo(String.valueOf(result.get("taskNo")));
            entity.setVersionNo((entity.getVersionNo() == null ? 0 : entity.getVersionNo()) + 1);
            entity.setGeneratedTime(new Date());
            entity.setStatus("READY");
            entity.setErrorMessage(null);
            updateById(entity);
            return R.ok(entity, "模拟音频生成成功");
        } catch (Exception e) {
            entity.setStatus("FAILED");
            entity.setErrorMessage(truncate(e.getMessage()));
            updateById(entity);
            if (e instanceof ServiceException serviceException) {
                throw serviceException;
            }
            throw new ServiceException("模拟音频生成失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public R<String> deleteAudio(Long id) {
        ChallengeAudio entity = requireAudio(id);
        Long referenceCount = hearingChallengeDao.selectCount(new LambdaQueryWrapper<HearingChallenge>()
                .eq(HearingChallenge::getAudioBankId, id)
                .eq(HearingChallenge::getIsDelete, 0));
        if (referenceCount != null && referenceCount > 0) {
            throw new ServiceException("该音频已被题目引用，不能删除；可将其停用");
        }
        entity.setIsDelete(1);
        updateById(entity);
        return R.ok("删除成功");
    }

    private void validate(ChallengeAudioDto dto) {
        if (dto == null) {
            throw new ServiceException("数据不能为空");
        }
        if (!StringUtils.hasText(dto.getAudioCode())) {
            throw new ServiceException("音频编码不能为空");
        }
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new ServiceException("音频名称不能为空");
        }
        if (dto.getSourceAssetId() == null) {
            throw new ServiceException("请上传原始音频");
        }
        FileAsset source = fileAssetService.getById(dto.getSourceAssetId());
        if (source == null || Integer.valueOf(1).equals(source.getIsDelete())) {
            throw new ServiceException("原始音频不存在");
        }
        if (!ALLOWED_CHANNELS.contains(dto.getNChannels())) {
            throw new ServiceException("挑战音频仅支持 2、4、8、16 通道");
        }
        if (!ALLOWED_CARRIERS.contains(dto.getCarrier())) {
            throw new ServiceException("载波类型无效");
        }
        if (dto.getFLo() == null || dto.getFHi() == null || dto.getFLo().compareTo(BigDecimal.ZERO) <= 0
                || dto.getFHi().compareTo(dto.getFLo()) <= 0) {
            throw new ServiceException("频率范围无效");
        }
        requireRange(dto.getEnvCut(), BigDecimal.valueOf(20), BigDecimal.valueOf(500), "包络截止频率");
        requireRange(dto.getSpread(), BigDecimal.ZERO, BigDecimal.ONE, "电流扩散");
        requireRange(dto.getNoiseLevel(), BigDecimal.ZERO, BigDecimal.ONE, "噪声强度");
    }

    private void validateEntity(ChallengeAudio entity) {
        ChallengeAudioDto dto = new ChallengeAudioDto();
        dto.setAudioCode(entity.getAudioCode());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setSourceAssetId(entity.getSourceAssetId());
        dto.setNChannels(entity.getNChannels());
        dto.setCarrier(entity.getCarrier());
        dto.setFLo(entity.getFLo());
        dto.setFHi(entity.getFHi());
        dto.setEnvCut(entity.getEnvCut());
        dto.setSpread(entity.getSpread());
        dto.setNoiseLevel(entity.getNoiseLevel());
        validate(dto);
    }

    private void ensureCodeUnique(String code, Long currentId) {
        LambdaQueryWrapper<ChallengeAudio> wrapper = new LambdaQueryWrapper<ChallengeAudio>()
                .eq(ChallengeAudio::getAudioCode, code.trim());
        if (currentId != null) {
            wrapper.ne(ChallengeAudio::getId, currentId);
        }
        if (count(wrapper) > 0) {
            throw new ServiceException("音频编码已存在，请更换后再保存", 409);
        }
    }

    private ChallengeAudio requireAudio(Long id) {
        if (id == null) {
            throw new ServiceException("音频ID不能为空");
        }
        ChallengeAudio entity = getOne(new LambdaQueryWrapper<ChallengeAudio>()
                .eq(ChallengeAudio::getId, id)
                .eq(ChallengeAudio::getIsDelete, 0));
        if (entity == null) {
            throw new ServiceException("音频素材不存在或已删除");
        }
        return entity;
    }

    private boolean generationChanged(ChallengeAudio old, ChallengeAudioDto current) {
        return !Objects.equals(old.getSourceAssetId(), current.getSourceAssetId())
                || !Objects.equals(old.getNChannels(), current.getNChannels())
                || !Objects.equals(old.getCarrier(), current.getCarrier())
                || decimalsDiffer(old.getFLo(), current.getFLo())
                || decimalsDiffer(old.getFHi(), current.getFHi())
                || decimalsDiffer(old.getEnvCut(), current.getEnvCut())
                || decimalsDiffer(old.getSpread(), current.getSpread())
                || decimalsDiffer(old.getNoiseLevel(), current.getNoiseLevel());
    }

    private boolean decimalsDiffer(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return !Objects.equals(left, right);
        }
        return left.compareTo(right) != 0;
    }

    private void requireRange(BigDecimal value, BigDecimal min, BigDecimal max, String label) {
        if (value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new ServiceException(label + "超出允许范围");
        }
    }

    private String truncate(String message) {
        if (!StringUtils.hasText(message)) {
            return "未知错误";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
