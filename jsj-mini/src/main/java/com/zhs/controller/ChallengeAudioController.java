package com.zhs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.zhs.dto.ChallengeAudioDto;
import com.zhs.exception.ServiceException;
import com.zhs.model.ChallengeAudio;
import com.zhs.service.ChallengeAudioService;
import com.zhs.util.PageQueryUtil;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/hearing/challenge/audio")
@Api(value = "听音挑战模拟音频库")
public class ChallengeAudioController {

    @Resource
    private ChallengeAudioService challengeAudioService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<Map<String, Object>> getListByPage(
            @RequestParam(value = "currentPage", defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "nChannels", required = false) Integer nChannels) {
        LambdaQueryWrapper<ChallengeAudio> lambda = new QueryWrapper<ChallengeAudio>().lambda()
                .eq(ChallengeAudio::getIsDelete, 0);
        if (StringUtils.isNotBlank(keyword)) {
            lambda.and(w -> w.like(ChallengeAudio::getTitle, keyword)
                    .or().like(ChallengeAudio::getAudioCode, keyword));
        }
        if (StringUtils.isNotBlank(status)) {
            lambda.eq(ChallengeAudio::getStatus, status);
        }
        if (nChannels != null) {
            lambda.eq(ChallengeAudio::getNChannels, nChannels);
        }
        lambda.orderByDesc(ChallengeAudio::getUpdateTime).orderByDesc(ChallengeAudio::getId);
        return R.ok(PageQueryUtil.queryPage(challengeAudioService, currentPage, pageSize, lambda));
    }

    @ApiOperation(value = "详情")
    @GetMapping("/getById")
    public R<ChallengeAudio> getById(@RequestParam("id") Long id) {
        ChallengeAudio entity = challengeAudioService.getOne(new LambdaQueryWrapper<ChallengeAudio>()
                .eq(ChallengeAudio::getId, id)
                .eq(ChallengeAudio::getIsDelete, 0));
        if (entity == null) {
            throw new ServiceException("音频素材不存在或已删除");
        }
        return R.ok(entity);
    }

    @ApiOperation(value = "新增或更新")
    @PostMapping("/addOrUpdate")
    public R<ChallengeAudio> addOrUpdate(@RequestBody ChallengeAudioDto dto) {
        return challengeAudioService.addOrUpdate(dto);
    }

    @ApiOperation(value = "生成模拟音频")
    @PostMapping("/generate/{id}")
    public R<ChallengeAudio> generate(@PathVariable Long id) {
        return challengeAudioService.generate(id);
    }

    @ApiOperation(value = "删除")
    @GetMapping("/delete/{id}")
    public R<String> delete(@PathVariable Long id) {
        return challengeAudioService.deleteAudio(id);
    }
}
