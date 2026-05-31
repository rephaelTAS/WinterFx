package com.ossobo.winterfx.notifications.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnConfirmation{
    String titulo() default "Confirmação";
    String descricao();
    String confirmText() default "Sim";
    String cancelText() default "Cancelar";
}
