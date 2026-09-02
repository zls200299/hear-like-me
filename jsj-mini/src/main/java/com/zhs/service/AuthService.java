package com.zhs.service;

import com.zhs.request.auth.WxLoginReq;
import com.zhs.request.auth.UpdateProfileReq;
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
     * 更新当前登录用户的昵称和头像
     */
    CurrentUserResp updateProfile(Long userId, UpdateProfileReq req);

    /**
     * 退出登录
     */
    void logout(String token);
}
