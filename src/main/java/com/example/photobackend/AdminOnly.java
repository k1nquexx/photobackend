package com.example.photobackend;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для защиты методов контроллеров.
 * Автоматически перехватывается AdminAuthInterceptor,
 * предотвращая вызов метода без действующего токена администратора.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminOnly {
}
