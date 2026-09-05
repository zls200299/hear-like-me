package com.zhs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.zhs.model.HearingChallengeAttempt;
import com.zhs.service.HearingChallengeAttemptService;
import com.zhs.util.PageQueryUtil;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/hearing/challenge/attempt")
@Api(value = "听音挑战答题记录")
@Slf4j
public class HearingChallengeAttemptController {

    @Resource
    private HearingChallengeAttemptService hearingChallengeAttemptService;

    @ApiOperation(value = "分页查询答题记录")
    @GetMapping("/getByPage")
    public R<Map<String, Object>> getListByPage(
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(value = "userKeyword", required = false) String userKeyword,
            @RequestParam(value = "audioKeyword", required = false) String audioKeyword,
            @RequestParam(value = "isCorrect", required = false) Integer isCorrect) {

        LambdaQueryWrapper<HearingChallengeAttempt> lambda = new QueryWrapper<HearingChallengeAttempt>().lambda();

        if (StringUtils.isNotBlank(userKeyword)) {
            String keyword = userKeyword.trim();
            if (keyword.matches("\\d+")) {
                Long userId = Long.parseLong(keyword);
                lambda.and(w -> w.like(HearingChallengeAttempt::getUserNickname, keyword)
                        .or()
                        .eq(HearingChallengeAttempt::getUserId, userId));
            } else {
                lambda.like(HearingChallengeAttempt::getUserNickname, keyword);
            }
        }
        if (StringUtils.isNotBlank(audioKeyword)) {
            String keyword = audioKeyword.trim();
            lambda.and(w -> w.like(HearingChallengeAttempt::getAudioTitle, keyword)
                    .or()
                    .like(HearingChallengeAttempt::getQuestionTitle, keyword)
                    .or()
                    .like(HearingChallengeAttempt::getQuestionCode, keyword));
        }
        if (isCorrect != null) {
            lambda.eq(HearingChallengeAttempt::getIsCorrect, isCorrect);
        }

        lambda.orderByDesc(HearingChallengeAttempt::getCreateTime)
                .orderByDesc(HearingChallengeAttempt::getId);

        return R.ok(PageQueryUtil.queryPage(hearingChallengeAttemptService, currentPage, pageSize, lambda));
    }
}
