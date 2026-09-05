package com.zhs.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口必须登录。
 * <p>
 * 可用于类或方法。方法级优先于类级；
 * 在类已标注 {@link NoLoginRequest} 时，仍可对个别方法强制登录。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireLogin {
}
