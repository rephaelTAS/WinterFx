package com.ossobo.winterfx.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca o parâmetro principal de dados de uma rota (Entidades, DTOs, Listas).
 *
 * <p>Equivalente ao {@code @RequestBody} do Spring, mas sem conversão de JSON,
 * já que o objeto Java real é passado diretamente na mesma JVM.</p>
 *
 * <p><b>Exemplo:</b> {@code @Payload Usuario usuario}</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Payload {
    /** A chave utilizada para buscar o objeto no mapa. Padrão é "payload". */
    String value() default "payload";
}