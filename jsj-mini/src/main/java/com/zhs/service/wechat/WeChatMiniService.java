package com.zhs.service.wechat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhs.config.WxMiniProperties;
import com.zhs.dto.wechat.WeChatSessionResult;
import com.zhs.exception.ServiceException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 微信小程序 code → openId 服务
 * <p>
 * 调微信 jscode2session 接口，仅使用接口响应中的真实 openId，禁止本地伪造。
 */
@Slf4j
@Service
public class WeChatMiniService {

    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    private final WxMiniProperties wxMiniProperties;
    private final Environment environment;

    public WeChatMiniService(WxMiniProperties wxMiniProperties, Environment environment) {
        this.wxMiniProperties = wxMiniProperties;
        this.environment = environment;
    }

    @PostConstruct
    void logWxBindingAtStartup() {
        String[] profiles = environment.getActiveProfiles();
        String profileStr = profiles.length > 0 ? String.join(",", profiles) : "(default)";
        String appId = wxMiniProperties.getAppId();
        String appSecret = wxMiniProperties.getAppSecret();
        log.info("[wx] 启动诊断 activeProfiles={} wx.mini.appId={} wx.mini.appSecret.empty={} appSecret.len={}",
                profileStr,
                appId != null ? appId : "null",
                appSecret == null || !StringUtils.hasText(appSecret),
                appSecret != null ? appSecret.length() : 0);
    }

    /**
     * 通过 wx.login() 返回的 code 换取 openId/unionId
     */
    public WeChatSessionResult getOpenIdByCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new ServiceException("code 不能为空");
        }

        String appId     = wxMiniProperties.getAppId();
        String appSecret = wxMiniProperties.getAppSecret();

        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            log.error("[wx code2Session] 微信配置未生效，请检查 application.yml 中 wx.mini.appId / wx.mini.appSecret");
            throw new ServiceException("服务端微信配置未就绪，请联系管理员");
        }

        String url = UriComponentsBuilder
                .fromUriString("https://api.weixin.qq.com/sns/jscode2session")
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build().toUriString();

        try {
            String body = REST_TEMPLATE.getForObject(url, String.class);
            if (!StringUtils.hasText(body)) {
                throw new ServiceException("微信接口无响应");
            }

            JSONObject root = JSON.parseObject(body);
            int errcode = root.getIntValue("errcode", 0);
            if (errcode != 0) {
                String errmsg = root.getString("errmsg");
                log.error("[wx code2Session] 微信业务错误 errcode={} errmsg={}", errcode, errmsg);
                throw new ServiceException("微信登录失败: " + errmsg);
            }

            String openId = root.getString("openid");
            if (!StringUtils.hasText(openId)) {
                log.error("[wx code2Session] 响应缺少 openid body={}", body);
                throw new ServiceException("微信响应缺少 openid");
            }

            WeChatSessionResult result = new WeChatSessionResult();
            result.setOpenId(openId);
            result.setUnionId(root.getString("unionid"));
            log.info("[wx code2Session] 成功 openId={} unionId.present={}", openId, result.getUnionId() != null);
            return result;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[wx code2Session] 调用异常", e);
            throw new ServiceException("微信登录异常: " + e.getMessage());
        }
    }
}
