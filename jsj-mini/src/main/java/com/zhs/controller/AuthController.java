package com.zhs.controller;

import com.zhs.common.NoLoginRequest;
import com.zhs.common.TokenUtil;
import com.zhs.common.UserContext;
import com.zhs.exception.ServiceException;
import com.zhs.request.auth.WxLoginReq;
import com.zhs.request.auth.UpdateProfileReq;
import com.zhs.response.auth.CurrentUserResp;
import com.zhs.response.auth.WxLoginResp;
import com.zhs.service.AuthService;
import com.zhs.util.R;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序认证接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @NoLoginRequest
    @PostMapping("/wx-login")
    public R<WxLoginResp> wxLogin(@RequestBody WxLoginReq req) {
        return R.ok(authService.wxLogin(req));
    }

    @GetMapping("/current-user")
    public R<CurrentUserResp> currentUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new ServiceException("未登录", 401);
        }
        return R.ok(authService.getCurrentUser(userId));
    }

    @PostMapping("/profile")
    public R<CurrentUserResp> updateProfile(@RequestBody UpdateProfileReq req) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new ServiceException("未登录", 401);
        }
        return R.ok(authService.updateProfile(userId, req));
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        authService.logout(TokenUtil.resolveFrom(request));
        return R.ok();
    }
}
