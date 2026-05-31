package com.ossobo.winterfx.notifications.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnSuccess {
    String titulo() default "Sucesso";
    String descricao();
    long duracao() default 3000;
}

