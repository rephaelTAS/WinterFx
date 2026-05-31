package com.ossobo.winterfx.notifications.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnInfo {
    String titulo() default "Informação";
    String descricao();
    String detalhes() default "";
    long duracao() default 0; // Não some sozinho
}
