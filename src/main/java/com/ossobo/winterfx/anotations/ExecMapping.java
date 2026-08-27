package com.ossobo.winterfx.anotations;

import java.lang.annotation.*;

/**
 * Rota de comando/processo. Namespace EXEC — livre para compartilhar
 * rota com qualquer outra anotação (GET, PUT, DELETE, UI).
 *
 * <p>Mesmo caminho + verbos diferentes = mesmo método, invocado pelos
 * canais {@code Rotas.get()}, {@code Rotas.put()} ou {@code Rotas.exec()}
 * conforme a intenção do chamador.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface ExecMapping {
    String value() default "";
}