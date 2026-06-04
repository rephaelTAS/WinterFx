package com.ossobo.winterfx.notifications.anotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnWarning {
    String titulo();
    String descricao() default "";
}