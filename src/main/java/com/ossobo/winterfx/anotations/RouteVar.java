package com.ossobo.winterfx.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Extrai um dado primitivo ou variável de contexto da rota.
 * Exemplo de uso no contexto: Params.with("id", 10L)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RouteVar {
    /** A chave usada para buscar esse valor no mapa de parâmetros. */
    String value();
}