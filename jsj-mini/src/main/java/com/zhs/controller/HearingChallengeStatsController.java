package com.zhs.controller;

import com.zhs.response.challenge.ChallengeStatsResp;
import com.zhs.service.HearingChallengeAttemptService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hearing/challenge/stats")
@Api(value = "听音挑战统计")
@Slf4j
public class HearingChallengeStatsController {

    @Resource
    private HearingChallengeAttemptService hearingChallengeAttemptService;

    @ApiOperation(value = "挑战统计看板")
    @GetMapping("/overview")
    public R<ChallengeStatsResp> overview(
            @RequestParam(value = "days", required = false, defaultValue = "7") Integer days) {
        return R.ok(hearingChallengeAttemptService.getStats(days));
    }
}
