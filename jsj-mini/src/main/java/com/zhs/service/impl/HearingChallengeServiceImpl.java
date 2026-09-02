package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.bean.BeanUtil;
import com.zhs.dao.HearingChallengeDao;
import com.zhs.dto.HearingChallengeDto;
import com.zhs.exception.ServiceException;
import com.zhs.model.FileAsset;
import com.zhs.model.ChallengeAudio;
import com.zhs.model.HearingChallenge;
import com.zhs.util.R;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.zhs.response.challenge.ChallengeQuestionDetailResp;
import com.zhs.response.challenge.ChallengeQuestionItemResp;
import com.zhs.response.challenge.ChallengeQuestionListResp;
import com.zhs.response.challenge.ChallengeSubmitAnswerResp;
import com.zhs.service.HearingChallengeService;
import com.zhs.service.ChallengeAudioService;
import com.zhs.service.IFileAssetService;
import com.zhs.service.storage.LocalFileStorageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Service
public class HearingChallengeServiceImpl extends ServiceImpl<HearingChallengeDao, HearingChallenge>
        implements HearingChallengeService {

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final Set<Integer> ALLOWED_CHANNELS = Set.of(2, 4, 8, 16);

    @Resource
    private IFileAssetService fileAssetService;

    @Resource
    private LocalFileStorageService localFileStorageService;

    @Resource
    private ChallengeAudioService challengeAudioService;

    @Override
    public R addOrUpdate(HearingChallengeDto dto) {
        if (BeanUtil.isEmpty(dto)) {
            throw new ServiceException("数据不能为空");
        }
        if (StringUtils.isBlank(dto.getQuestionCode())) {
            throw new ServiceException("题目编码不能为空");
        }
        if (StringUtils.isBlank(dto.getTitle())) {
            throw new ServiceException("题目标题不能为空");
        }
        ensureQuestionCodeUnique(dto.getQuestionCode().trim(), dto.getId());
        HearingChallenge entity = new HearingChallenge();
        BeanUtil.copyProperties(dto, entity);
        entity.setQuestionCode(dto.getQuestionCode().trim());
        applyAudioSnapshot(dto, entity);
        if (entity.getIsDelete() == null) {
            entity.setIsDelete(0);
        }
        if ("PUBLISHED".equals(entity.getStatus()) && entity.getPublishedTime() == null) {
            entity.setPublishedTime(new Date());
        }

        if (dto.getId() == null) {
            save(entity);
            return R.ok("数据插入成功");
        }
        updateById(entity);
        return R.ok("数据更新成功");
    }

    private void ensureQuestionCodeUnique(String questionCode, Long currentId) {
        LambdaQueryWrapper<HearingChallenge> wrapper = new LambdaQueryWrapper<HearingChallenge>()
                .eq(HearingChallenge::getQuestionCode, questionCode);
        if (currentId != null) {
            wrapper.ne(HearingChallenge::getId, currentId);
        }
        if (count(wrapper) > 0) {
            throw new ServiceException("题目编码已存在，请更换后再保存", 409);
        }
    }

    private void applyAudioSnapshot(HearingChallengeDto dto, HearingChallenge entity) {
        if (dto.getAudioBankId() == null) {
            if (dto.getId() != null) {
                HearingChallenge existing = getById(dto.getId());
                if (existing != null && existing.getAudioBankId() == null) {
                    entity.setAudioAssetId(existing.getAudioAssetId());
                    entity.setNChannels(existing.getNChannels());
                    entity.setCarrier(existing.getCarrier());
                    entity.setFLo(existing.getFLo());
                    entity.setFHi(existing.getFHi());
                    entity.setEnvCut(existing.getEnvCut());
                    entity.setSpread(existing.getSpread());
                    entity.setNoiseLevel(existing.getNoiseLevel());
                    entity.setEnvAmp(existing.getEnvAmp());
                    entity.setWetMix(existing.getWetMix());
                    entity.setCompressEnabled(existing.getCompressEnabled());
                    entity.setNormalizePeak(existing.getNormalizePeak());
                    return;
                }
            }
            throw new ServiceException("请选择已生成的模拟音频");
        }

        ChallengeAudio audio = challengeAudioService.getOne(new LambdaQueryWrapper<ChallengeAudio>()
                .eq(ChallengeAudio::getId, dto.getAudioBankId())
                .eq(ChallengeAudio::getStatus, "READY")
                .eq(ChallengeAudio::getIsDelete, 0));
        if (audio == null || audio.getOutputAssetId() == null) {
            throw new ServiceException("所选模拟音频不可用，请先生成成功");
        }
        entity.setAudioBankId(audio.getId());
        entity.setAudioAssetId(audio.getOutputAssetId());
        entity.setNChannels(audio.getNChannels());
        entity.setCarrier(audio.getCarrier());
        entity.setFLo(audio.getFLo());
        entity.setFHi(audio.getFHi());
        entity.setEnvCut(audio.getEnvCut());
        entity.setSpread(audio.getSpread());
        entity.setNoiseLevel(audio.getNoiseLevel());
    }

    @Override
    public ChallengeQuestionListResp listPublishedQuestions() {
        List<HearingChallenge> questions = listPublishedOrdered();
        ChallengeQuestionListResp resp = new ChallengeQuestionListResp();
        resp.setTotal(questions.size());
        resp.setItems(IntStream.range(0, questions.size())
                .mapToObj(i -> toListItem(questions.get(i), i + 1))
                .toList());
        return resp;
    }

    @Override
    public ChallengeQuestionDetailResp getCurrentQuestion(Integer index) {
        int questionIndex = index == null || index < 1 ? 1 : index;
        List<HearingChallenge> questions = listPublishedOrdered();
        if (questions.isEmpty()) {
            throw new ServiceException("暂无已发布的听音挑战题目");
        }
        if (questionIndex > questions.size()) {
            throw new ServiceException("题目序号超出范围");
        }
        return toDetail(questions.get(questionIndex - 1), questionIndex, questions.size());
    }

    @Override
    public ChallengeQuestionDetailResp getQuestionById(Long questionId) {
        HearingChallenge question = requirePublishedQuestion(questionId);
        List<HearingChallenge> questions = listPublishedOrdered();
        int questionIndex = IntStream.range(0, questions.size())
                .filter(i -> questions.get(i).getId().equals(question.getId()))
                .map(i -> i + 1)
                .findFirst()
                .orElseThrow(() -> new ServiceException("题目不存在"));
        return toDetail(question, questionIndex, questions.size());
    }

    @Override
    public ChallengeSubmitAnswerResp submitAnswer(Long questionId, Integer selectedChannels) {
        if (questionId == null) {
            throw new ServiceException("questionId 不能为空");
        }
        if (selectedChannels == null || !ALLOWED_CHANNELS.contains(selectedChannels)) {
            throw new ServiceException("请选择有效的通道数");
        }

        HearingChallenge question = requirePublishedQuestion(questionId);
        List<HearingChallenge> questions = listPublishedOrdered();
        int questionIndex = IntStream.range(0, questions.size())
                .filter(i -> questions.get(i).getId().equals(question.getId()))
                .map(i -> i + 1)
                .findFirst()
                .orElseThrow(() -> new ServiceException("题目不存在"));

        boolean correct = question.getNChannels() != null && question.getNChannels().equals(selectedChannels);

        ChallengeSubmitAnswerResp resp = new ChallengeSubmitAnswerResp();
        resp.setQuestionId(question.getId());
        resp.setSelectedChannels(selectedChannels);
        resp.setCorrectChannels(question.getNChannels());
        resp.setCorrect(correct);
        resp.setTip(correct ? question.getCorrectTip() : question.getWrongTip());
        resp.setTotal(questions.size());

        if (questionIndex < questions.size()) {
            HearingChallenge next = questions.get(questionIndex);
            resp.setHasNext(true);
            resp.setNextQuestionId(next.getId());
            resp.setNextIndex(questionIndex + 1);
        } else {
            resp.setHasNext(false);
            resp.setNextQuestionId(null);
            resp.setNextIndex(null);
        }
        return resp;
    }

    private List<HearingChallenge> listPublishedOrdered() {
        LambdaQueryWrapper<HearingChallenge> wrapper = new LambdaQueryWrapper<HearingChallenge>()
                .eq(HearingChallenge::getStatus, STATUS_PUBLISHED)
                .eq(HearingChallenge::getIsDelete, 0)
                .orderByAsc(HearingChallenge::getSortOrder)
                .orderByAsc(HearingChallenge::getId);
        return list(wrapper);
    }

    private HearingChallenge requirePublishedQuestion(Long questionId) {
        HearingChallenge question = getOne(new LambdaQueryWrapper<HearingChallenge>()
                .eq(HearingChallenge::getId, questionId)
                .eq(HearingChallenge::getStatus, STATUS_PUBLISHED)
                .eq(HearingChallenge::getIsDelete, 0)
                .last("limit 1"));
        if (question == null) {
            throw new ServiceException("题目不存在或未发布");
        }
        return question;
    }

    private ChallengeQuestionItemResp toListItem(HearingChallenge question, int index) {
        ChallengeQuestionItemResp item = new ChallengeQuestionItemResp();
        item.setId(question.getId());
        item.setQuestionCode(question.getQuestionCode());
        item.setTitle(question.getTitle());
        item.setDescription(question.getDescription());
        item.setSortOrder(question.getSortOrder());
        item.setIndex(index);
        return item;
    }

    private ChallengeQuestionDetailResp toDetail(HearingChallenge question, int index, int total) {
        FileAsset asset = fileAssetService.getById(question.getAudioAssetId());
        if (asset == null || (asset.getIsDelete() != null && asset.getIsDelete() == 1)) {
            throw new ServiceException("题目音频不存在");
        }

        ChallengeQuestionDetailResp resp = new ChallengeQuestionDetailResp();
        resp.setId(question.getId());
        resp.setQuestionCode(question.getQuestionCode());
        resp.setTitle(question.getTitle());
        resp.setDescription(question.getDescription());
        resp.setAudioAssetId(question.getAudioAssetId());
        resp.setAudioUrl(localFileStorageService.buildPreviewUrl(question.getAudioAssetId()));
        resp.setIndex(index);
        resp.setTotal(total);
        return resp;
    }
}
