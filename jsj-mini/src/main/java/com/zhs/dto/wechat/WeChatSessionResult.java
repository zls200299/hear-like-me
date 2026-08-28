package com.zhs.dto.wechat;

import lombok.Data;

/**
 * 微信 jscode2session 解析结果（仅使用接口返回的 openid，不用 code/时间戳拼接）
 */
@Data
public class WeChatSessionResult {

    private String openId;
    /** 同主体下可能有，未返回则为 null */
    private String unionId;
}
