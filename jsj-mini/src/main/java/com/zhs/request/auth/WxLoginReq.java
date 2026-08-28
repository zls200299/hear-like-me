package com.zhs.request.auth;

import lombok.Data;

/**
 * 微信小程序登录请求
 */
@Data
public class WxLoginReq {

    /** 微信临时 code（wx.login 返回）*/
    private String code;

    /** 用户昵称（可选，首次登录时传入）*/
    private String nickname;

    /** 用户头像（可选，首次登录时传入）*/
    private String avatar;
}
