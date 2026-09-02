package com.zhs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.zhs.exception.ServiceException;
import com.zhs.model.User;
import com.zhs.model.UserSensitiveInfo;
import com.zhs.service.IUserSensitiveInfoService;
import com.zhs.service.IUserService;
import com.zhs.util.PageQueryUtil;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台：小程序用户管理
 */
@RestController
@RequestMapping("/user/mini")
@Api(value = "小程序用户管理")
public class MiniUserAdminController {

    private static final int SOURCE_MINI_PROGRAM = 3;

    @Resource
    private IUserService userService;

    @Resource
    private IUserSensitiveInfoService userSensitiveInfoService;

    @ApiOperation(value = "分页查询小程序用户")
    @GetMapping("/getByPage")
    public R<Map<String, Object>> getListByPage(
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "status", required = false) Integer status) {
        LambdaQueryWrapper<User> wrapper = new QueryWrapper<User>().lambda()
                .eq(User::getIsDelete, 0)
                .eq(User::getSourceType, SOURCE_MINI_PROGRAM);
        if (StringUtils.isNotBlank(nickname)) {
            wrapper.like(User::getNickname, nickname.trim());
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getRegisterTime);

        Map<String, Object> page = PageQueryUtil.queryPage(userService, currentPage, pageSize, wrapper);
        @SuppressWarnings("unchecked")
        List<User> records = (List<User>) page.get("records");
        page.put("records", toListItems(records));
        return R.ok(page);
    }

    @ApiOperation(value = "小程序用户详情")
    @GetMapping("/getById")
    public R<Map<String, Object>> getById(@RequestParam("id") String id) {
        if (StringUtils.isBlank(id)) {
            throw new ServiceException("id不能为空");
        }
        User user = userService.getOne(new QueryWrapper<User>().lambda()
                .eq(User::getId, id)
                .eq(User::getIsDelete, 0)
                .eq(User::getSourceType, SOURCE_MINI_PROGRAM));
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return R.ok(toDetail(user));
    }

    @ApiOperation(value = "更新用户状态")
    @PostMapping("/updateStatus")
    @Transactional
    public R updateStatus(@RequestBody Map<String, Object> body) {
        Object idObj = body.get("id");
        Object statusObj = body.get("status");
        if (idObj == null || statusObj == null) {
            throw new ServiceException("id 与 status 不能为空");
        }
        int status = Integer.parseInt(String.valueOf(statusObj));
        if (status != 0 && status != 1) {
            throw new ServiceException("status 只能为 0 或 1");
        }
        User user = userService.getById(String.valueOf(idObj));
        if (user == null || user.getIsDelete() != null && user.getIsDelete() == 1) {
            throw new ServiceException("用户不存在");
        }
        if (user.getSourceType() == null || user.getSourceType() != SOURCE_MINI_PROGRAM) {
            throw new ServiceException("仅支持操作小程序用户");
        }
        user.setStatus(status);
        userService.updateById(user);
        return R.ok("状态更新成功");
    }

    private List<Map<String, Object>> toListItems(List<User> users) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (users == null) {
            return rows;
        }
        for (User user : users) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", String.valueOf(user.getId()));
            row.put("nickname", user.getNickname());
            row.put("avatar", user.getAvatar());
            row.put("status", user.getStatus());
            row.put("sourceType", user.getSourceType());
            row.put("registerTime", user.getRegisterTime());
            row.put("lastActiveTime", user.getLastActiveTime());
            UserSensitiveInfo sensitive = getSensitiveInfo(user.getId());
            row.put("openIdMasked", maskOpenId(sensitive == null ? null : sensitive.getOpenId()));
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> toDetail(User user) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", String.valueOf(user.getId()));
        detail.put("nickname", user.getNickname());
        detail.put("avatar", user.getAvatar());
        detail.put("bio", user.getBio());
        detail.put("status", user.getStatus());
        detail.put("sourceType", user.getSourceType());
        detail.put("registerTime", user.getRegisterTime());
        detail.put("lastActiveTime", user.getLastActiveTime());
        detail.put("createTime", user.getCreateTime());
        detail.put("updateTime", user.getUpdateTime());

        UserSensitiveInfo sensitive = getSensitiveInfo(user.getId());
        if (sensitive != null) {
            detail.put("openIdMasked", maskOpenId(sensitive.getOpenId()));
            detail.put("unionIdMasked", maskOpenId(sensitive.getUnionId()));
            detail.put("miniAppId", sensitive.getMiniAppId());
        }
        return detail;
    }

    private UserSensitiveInfo getSensitiveInfo(Long userId) {
        if (userId == null) {
            return null;
        }
        return userSensitiveInfoService.getOne(new QueryWrapper<UserSensitiveInfo>().lambda()
                .eq(UserSensitiveInfo::getUserId, userId)
                .eq(UserSensitiveInfo::getIsDelete, 0)
                .last("limit 1"));
    }

    private String maskOpenId(String openId) {
        if (StringUtils.isBlank(openId)) {
            return "-";
        }
        if (openId.length() <= 8) {
            return openId.charAt(0) + "***" + openId.charAt(openId.length() - 1);
        }
        return openId.substring(0, 4) + "***" + openId.substring(openId.length() - 4);
    }
}
