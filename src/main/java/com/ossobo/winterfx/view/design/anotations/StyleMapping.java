package com.ossobo.winterfx.view.design.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StyleMapping {
    String id() default "";
    String[] classes() default {};
    String condition() default "";
}
