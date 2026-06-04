package com.ossobo.winterfx.view.anotations;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NewScene {
    String view();              // ID da view
    double width() default -1;  // -1 = usa tamanho da view
    double height() default -1; // -1 = usa tamanho da view
    boolean closeCurrent() default true;
    String title() default "";
    boolean centered() default true;
}