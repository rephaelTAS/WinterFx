package com.ossobo.winterfx.imagemanager.anotations;

import java.lang.annotation.*;

/**
 * Container para múltiplas anotações {@link RegisterImage}.
 *
 * <p>Permite registrar várias imagens em uma única classe.</p>
 *
 * <pre>
 * {@code
 * @RegisterImages({
 *     @RegisterImage(id = "logo", src = "/images/logo.png"),
 *     @RegisterImage(id = "banner", src = "/images/banner.png",
 *                    preferredWidth = 800, preferredHeight = 200)
 * })
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
@Documented
public @interface RegisterImages {
    RegisterImage[] value();
}