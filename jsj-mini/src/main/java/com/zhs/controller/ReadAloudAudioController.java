package com.zhs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.zhs.dto.ReadAloudAudioDto;
import com.zhs.exception.ServiceException;
import com.zhs.model.ReadAloudAudio;
import com.zhs.service.ReadAloudAudioService;
import com.zhs.util.PageQueryUtil;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/read/aloud/audio")
@Api(value = "点读音频库")
public class ReadAloudAudioController {

    @Resource
    private ReadAloudAudioService readAloudAudioService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<Map<String, Object>> getListByPage(
            @RequestParam(value = "currentPage", defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "nChannels", required = false) Integer nChannels) {
        LambdaQueryWrapper<ReadAloudAudio> lambda = new QueryWrapper<ReadAloudAudio>().lambda()
                .eq(ReadAloudAudio::getIsDelete, 0);
        if (StringUtils.isNotBlank(keyword)) {
            lambda.and(w -> w.like(ReadAloudAudio::getTitle, keyword)
                    .or().like(ReadAloudAudio::getAudioCode, keyword));
        }
        if (StringUtils.isNotBlank(status)) {
            lambda.eq(ReadAloudAudio::getStatus, status);
        }
        if (nChannels != null) {
            lambda.eq(ReadAloudAudio::getNChannels, nChannels);
        }
        lambda.orderByDesc(ReadAloudAudio::getUpdateTime).orderByDesc(ReadAloudAudio::getId);
        return R.ok(PageQueryUtil.queryPage(readAloudAudioService, currentPage, pageSize, lambda));
    }

    @ApiOperation(value = "详情")
    @GetMapping("/getById")
    public R<ReadAloudAudio> getById(@RequestParam("id") Long id) {
        ReadAloudAudio entity = readAloudAudioService.getOne(new LambdaQueryWrapper<ReadAloudAudio>()
                .eq(ReadAloudAudio::getId, id)
                .eq(ReadAloudAudio::getIsDelete, 0));
        if (entity == null) {
            throw new ServiceException("音频素材不存在或已删除");
        }
        return R.ok(entity);
    }

    @ApiOperation(value = "新增或更新")
    @PostMapping("/addOrUpdate")
    public R<ReadAloudAudio> addOrUpdate(@RequestBody ReadAloudAudioDto dto) {
        return readAloudAudioService.addOrUpdate(dto);
    }

    @ApiOperation(value = "生成模拟音频")
    @PostMapping("/generate/{id}")
    public R<ReadAloudAudio> generate(@PathVariable Long id) {
        return readAloudAudioService.generate(id);
    }

    @ApiOperation(value = "删除")
    @GetMapping("/delete/{id}")
    public R<String> delete(@PathVariable Long id) {
        return readAloudAudioService.deleteAudio(id);
    }
}
