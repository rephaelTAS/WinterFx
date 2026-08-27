package com.ossobo.winterfx.anotations;

import java.lang.annotation.*;

/**
 * Rota de remoção. Enfileirada exclusivamente no namespace DELETE.
 *
 * <p><b>Regra de segurança:</b> não pode coexistir com {@code @GetMapping}
 * nem {@code @PutMapping} na mesma rota.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface DeleteMapping {
    String value() default "";
}