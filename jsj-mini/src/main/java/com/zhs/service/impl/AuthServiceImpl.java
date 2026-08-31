package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.cachekey.user.TokenCacheKey;
import com.zhs.model.User;
import com.zhs.model.UserSensitiveInfo;
import com.zhs.dto.wechat.WeChatSessionResult;
import com.zhs.exception.ServiceException;
import com.zhs.mapper.UserMapper;
import com.zhs.mapper.UserSensitiveInfoMapper;
import com.zhs.request.auth.WxLoginReq;
import com.zhs.response.auth.CurrentUserResp;
import com.zhs.response.auth.WxLoginResp;
import com.zhs.service.AuthService;
import com.zhs.service.wechat.WeChatMiniService;
import com.zhs.util.MiniRedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.UUID;

/**
 * 认证 Service 实现
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserSensitiveInfoMapper sensitiveInfoMapper;

    @Resource
    private WeChatMiniService weChatMiniService;

    @Resource
    private MiniRedisUtil miniRedisUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WxLoginResp wxLogin(WxLoginReq req) {
        // 1. code → 微信 openId（禁止本地伪造）
        WeChatSessionResult wx = weChatMiniService.getOpenIdByCode(req.getCode());
        String openId   = wx.getOpenId();
        String unionId  = wx.getUnionId();
        log.info("[wx-login] code2Session 完成 openId={} unionId.present={}", openId, unionId != null);

        // 2. 按 openId 查 user_sensitive_info，判断新老用户
        UserSensitiveInfo sensitiveInfo = sensitiveInfoMapper.selectOne(
                new LambdaQueryWrapper<UserSensitiveInfo>()
                        .eq(UserSensitiveInfo::getOpenId, openId)
                        .eq(UserSensitiveInfo::getIsDelete, 0)
                        .last("limit 1")
        );

        User user;
        if (sensitiveInfo == null) {
            // 3a. 新用户 → 创建用户 + 敏感信息
            log.info("[wx-login] 新用户，openId 首次出现，开始注册");
            user = createNewUser(req, openId, unionId);
        } else {
            // 3b. 老用户 → 补全 unionId（若之前未绑定）
            log.info("[wx-login] 老用户 userId={}", sensitiveInfo.getUserId());
            user = userMapper.selectById(sensitiveInfo.getUserId());
            if (user == null || user.getIsDelete() == 1) {
                throw new ServiceException("账号异常，请联系客服");
            }
            if (StringUtils.hasText(unionId) && !StringUtils.hasText(sensitiveInfo.getUnionId())) {
                sensitiveInfo.setUnionId(unionId);
                sensitiveInfoMapper.updateById(sensitiveInfo);
            }
            applyProfileUpdate(user, req);
        }

        // 4. 生成 token 写入 Redis（自动 30 天过期）
        String token = UUID.randomUUID().toString().replace("-", "");
        miniRedisUtil.set(new TokenCacheKey(token), user.getId());
        log.info("[wx-login] 登录成功 userId={} 新用户={}", user.getId(), sensitiveInfo == null);

        // 5. 返回
        WxLoginResp resp = new WxLoginResp();
        resp.setToken(token);
        resp.setUserId(user.getId());
        resp.setNickname(user.getNickname());
        resp.setAvatar(user.getAvatar());
        return resp;
    }

    @Override
    public CurrentUserResp getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDelete() == 1) {
            throw new ServiceException("用户不存在");
        }
        CurrentUserResp resp = new CurrentUserResp();
        resp.setUserId(user.getId());
        resp.setNickname(user.getNickname());
        resp.setAvatar(user.getAvatar());
        return resp;
    }

    @Override
    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            miniRedisUtil.delete(new TokenCacheKey(token));
            log.info("[logout] token 已清除");
        }
    }

    // ==================== private ====================

    private User createNewUser(WxLoginReq req, String openId, String unionId) {
        Date now = new Date();
        User user = new User();
        user.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname() : "用户" + System.currentTimeMillis());
        user.setAvatar(StringUtils.hasText(req.getAvatar()) ? req.getAvatar() : null);
        user.setStatus(1);
        user.setSourceType(3);
        user.setRegisterTime(now);
        user.setLastActiveTime(now);
        user.setIsDelete(0);
        userMapper.insert(user);

        // 创建 user_sensitive_info
        UserSensitiveInfo info = new UserSensitiveInfo();
        info.setUserId(user.getId());
        info.setOpenId(openId);
        // miniAppId 来自配置，暂不设置，业务层按需补充
        // info.setMiniAppId(wxMiniProperties.getAppId());
        if (StringUtils.hasText(unionId)) {
            info.setUnionId(unionId);
        }
        info.setIsDelete(0);
        sensitiveInfoMapper.insert(info);

        log.info("[wx-login] 新用户创建完成 userId={} openId={}", user.getId(), openId);
        return user;
    }

    private void applyProfileUpdate(User user, WxLoginReq req) {
        boolean updated = false;
        if (StringUtils.hasText(req.getNickname()) && !req.getNickname().equals(user.getNickname())) {
            user.setNickname(req.getNickname());
            updated = true;
        }
        if (StringUtils.hasText(req.getAvatar()) && !req.getAvatar().equals(user.getAvatar())) {
            user.setAvatar(req.getAvatar());
            updated = true;
        }
        if (updated) {
            user.setLastActiveTime(new Date());
            userMapper.updateById(user);
        }
    }
}
