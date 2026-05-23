package com.ossobo.winterfx.di.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE) // Ou ElementType.TYPE, ElementType.METHOD, dependendo de como você usa
public @interface Singleton {
    // Nada aqui, se for apenas uma anotação de marcador
}
