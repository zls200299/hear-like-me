package com.zhs.request.auth;

import lombok.Data;

@Data
public class MiniLoginReq {

    private String code;

    private String nickname;

    private String avatar;
}
