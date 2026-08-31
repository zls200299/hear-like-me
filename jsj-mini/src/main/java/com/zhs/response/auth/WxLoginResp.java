package com.zhs.response.auth;

import lombok.Data;

/**
 * 登录响应
 */
@Data
public class WxLoginResp {

    /** 登录 token */
    private String token;

    /** 用户主键 ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 是否新用户（首次 openId 注册） */
    private Boolean newUser;
}
