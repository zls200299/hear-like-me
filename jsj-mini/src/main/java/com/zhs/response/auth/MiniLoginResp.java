package com.zhs.response.auth;

import lombok.Data;

@Data
public class MiniLoginResp {

    private String token;

    private Long userId;

    private String userIdStr;

    private String nickname;

    private String avatar;
}
