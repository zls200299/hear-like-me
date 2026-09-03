package com.zhs.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private LoginInterceptor loginInterceptor;

    @Resource
    private AdminLoginInterceptor adminLoginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/wx-login",
                        "/api/auth/test-login",
                        "/api/auth/test-users");

        registry.addInterceptor(adminLoginInterceptor)
                .addPathPatterns(
                        "/audio/processing/**",
                        "/content/**",
                        "/file/asset/**",
                        "/hearing/challenge/**",
                        "/read/aloud/**",
                        "/sample/audio/**",
                        "/scenario/preset/**",
                        "/system/config/**",
                        "/user/**");
    }
}
