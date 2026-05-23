package com.ossobo.winterfx.di.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ComponentScan {
    String[] value() default {};  // Pacotes a escanear
}