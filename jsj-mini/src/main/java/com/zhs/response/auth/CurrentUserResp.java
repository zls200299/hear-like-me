package com.zhs.response.auth;

import lombok.Data;

/**
 * 当前登录用户信息
 */
@Data
public class CurrentUserResp {

    private Long userId;

    private String nickname;

    private String avatar;
}
