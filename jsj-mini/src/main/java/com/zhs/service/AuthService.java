package com.zhs.service;

import com.zhs.request.auth.WxLoginReq;
import com.zhs.response.auth.CurrentUserResp;
import com.zhs.response.auth.WxLoginResp;

/**
 * 认证 Service
 */
public interface AuthService {

    /**
     * 微信小程序登录
     */
    WxLoginResp wxLogin(WxLoginReq req);

    /**
     * 获取当前登录用户信息
     */
    CurrentUserResp getCurrentUser(Long userId);

    /**
     * 退出登录
     */
    void logout(String token);
}
