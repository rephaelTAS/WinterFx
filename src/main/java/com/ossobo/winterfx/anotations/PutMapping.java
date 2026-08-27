package com.ossobo.winterfx.anotations;

import java.lang.annotation.*;

/**
 * Rota de escrita/atualização. Enfileirada exclusivamente no namespace PUT.
 *
 * <p><b>Regra de segurança:</b> não pode coexistir com {@code @GetMapping}
 * nem {@code @DeleteMapping} na mesma rota. Pode coexistir com
 * {@code @ExecMapping} e {@code @UiMapping}, pois possuem namespaces próprios.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface PutMapping {
    String value() default "";
}