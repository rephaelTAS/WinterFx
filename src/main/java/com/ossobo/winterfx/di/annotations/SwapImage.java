package com.ossobo.winterfx.di.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface SwapImage {
    String imageView();
    String imageId();
    double width() default 0;
    double height() default 0;
    boolean before() default true;
}