package com.zhs.request.auth;

import lombok.Data;

/**
 * 小程序用户资料更新请求
 */
@Data
public class UpdateProfileReq {

    /** 用户昵称 */
    private String nickname;

    /** 已上传头像的访问地址 */
    private String avatar;
}
