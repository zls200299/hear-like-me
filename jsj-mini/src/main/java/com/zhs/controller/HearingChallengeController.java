package com.zhs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.zhs.dto.HearingChallengeDto;
import com.zhs.exception.ServiceException;
import com.zhs.model.HearingChallenge;
import com.zhs.service.HearingChallengeService;
import com.zhs.util.PageQueryUtil;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hearing/challenge")
@Api(value = "听音挑战题目管理")
@Slf4j
public class HearingChallengeController {

    @Resource
    private HearingChallengeService hearingChallengeService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<Map<String, Object>> getListByPage(
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "status", required = false) String status) {
        LambdaQueryWrapper<HearingChallenge> lambda = new QueryWrapper<HearingChallenge>().lambda()
                .eq(HearingChallenge::getIsDelete, 0);
        if (StringUtils.isNotBlank(title)) {
            lambda.like(HearingChallenge::getTitle, title);
        }
        if (StringUtils.isNotBlank(status)) {
            lambda.eq(HearingChallenge::getStatus, status);
        }
        lambda.orderByAsc(HearingChallenge::getSortOrder).orderByAsc(HearingChallenge::getId);
        return R.ok(PageQueryUtil.queryPage(hearingChallengeService, currentPage, pageSize, lambda));
    }

    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<HearingChallenge> getById(@RequestParam("id") String id) {
        if (StringUtils.isBlank(id)) {
            throw new ServiceException("id不能为空");
        }
        LambdaQueryWrapper<HearingChallenge> wrapper = new QueryWrapper<HearingChallenge>().lambda()
                .eq(HearingChallenge::getId, id)
                .eq(HearingChallenge::getIsDelete, 0);
        return R.ok(hearingChallengeService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id) {
        if (StringUtils.isBlank(id)) {
            throw new ServiceException("id不能为空");
        }
        HearingChallenge question = hearingChallengeService.getOne(new QueryWrapper<HearingChallenge>().lambda()
                .eq(HearingChallenge::getId, id)
                .eq(HearingChallenge::getIsDelete, 0));
        if (ObjectUtils.isEmpty(question)) {
            throw new ServiceException("该数据不存在或者已经被删除");
        }
        question.setIsDelete(1);
        hearingChallengeService.updateById(question);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody HearingChallengeDto dto) {
        if (CollectionUtils.isEmpty(dto.getIdList())) {
            throw new ServiceException("要删除的id不能为空!");
        }
        List<HearingChallenge> list = new ArrayList<>();
        dto.getIdList().forEach(id -> {
            HearingChallenge question = hearingChallengeService.getById(id);
            if (ObjectUtils.isEmpty(question)) {
                throw new ServiceException("id为" + id + "的数据不存在");
            }
            if (question.getIsDelete() != null && question.getIsDelete() == 1) {
                throw new ServiceException("id为" + id + "的数据已经被删除");
            }
            question.setIsDelete(1);
            list.add(question);
        });
        hearingChallengeService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody HearingChallengeDto dto) {
        return hearingChallengeService.addOrUpdate(dto);
    }
}
