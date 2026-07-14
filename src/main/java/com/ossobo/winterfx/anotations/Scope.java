package com.ossobo.winterfx.anotations;





import com.ossobo.winterfx.scanner.enums.ScopeType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Scope {
    ScopeType value() default ScopeType.SINGLETON;
}

