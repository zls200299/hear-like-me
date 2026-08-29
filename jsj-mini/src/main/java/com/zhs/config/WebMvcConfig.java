package com.zhs.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/wx-login",
                        "/api/auth/test-login",
                        "/api/auth/test-users");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 音频文件本地访问映射
        registry.addResourceHandler("/files/audio/**")
                .addResourceLocations("file:D:/hear-like-me/data/audio/");
    }
}
