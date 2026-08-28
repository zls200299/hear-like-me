package com.zhs.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.NoLoginRequest;
import com.zhs.common.TokenUtil;
import com.zhs.common.UserContext;
import com.zhs.model.UserLoginSession;
import com.zhs.mapper.UserLoginSessionMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Date;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private UserLoginSessionMapper loginSessionMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 检查是否有@NoLoginRequest注解
        NoLoginRequest methodAnnotation = handlerMethod.getMethodAnnotation(NoLoginRequest.class);
        NoLoginRequest classAnnotation = handlerMethod.getBeanType().getAnnotation(NoLoginRequest.class);

        if (methodAnnotation != null || classAnnotation != null) {
            return true;
        }

        // 获取 token：Authorization（可带 Bearer）、或 header「token」（小程序等场景）
        String token = TokenUtil.resolveFrom(request);
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\"}");
            return false;
        }

        // 从数据库查询token
        LambdaQueryWrapper<UserLoginSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLoginSession::getToken, token);
        wrapper.eq(UserLoginSession::getStatus, 1);
        wrapper.eq(UserLoginSession::getIsDelete, 0);
        UserLoginSession session = loginSessionMapper.selectOne(wrapper);

        if (session == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期\"}");
            return false;
        }

        // 检查是否过期
        Date expireTime = session.getExpireTime();
        if (expireTime == null || expireTime.before(new Date())) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期\"}");
            return false;
        }

        // 设置到上下文
        UserContext.setUserId(session.getUserId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }

}
