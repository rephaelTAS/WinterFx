package com.ossobo.winterfx.imagemanager.anotations;

import java.lang.annotation.*;

/**
 * Container para múltiplas anotações {@link InjectImage}.
 *
 * <p>Útil quando você precisa injetar várias imagens
 * e não pode usar anotações repetidas no mesmo campo.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface InjectImages {
    InjectImage[] value();
}