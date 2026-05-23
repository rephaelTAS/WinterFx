package com.ossobo.winterfx.di.annotations;





import com.ossobo.winterfx.di.scopes.enums.ScopeType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ScopeAnnotation {
    ScopeType value() default ScopeType.SINGLETON;
}

