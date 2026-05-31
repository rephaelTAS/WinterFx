package com.ossobo.winterfx.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indica que um bean deve ser dado como preferencial quando múltiplos beans
 * do mesmo tipo estão disponíveis para injeção e não há um qualificador explícito.
 * Se houver mais de um bean @Primary para o mesmo tipo, isso resultará em uma exceção
 * de ambiguidade no momento da resolução.
 */
@Target({ElementType.TYPE}) // Pode ser aplicada a classes/tipos
@Retention(RetentionPolicy.RUNTIME) // Disponível em tempo de execução
public @interface Primary {
}
