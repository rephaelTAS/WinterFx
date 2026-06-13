package com.ossobo.winterfx.view.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para identificar parâmetros de métodos @FXMLAction.
 * Permite diferenciar múltiplos parâmetros do mesmo tipo.
 *
 * Exemplo:
 * {@code @FXMLAction("salvar")
 * public void salvar(@FxArg("nome") String nome, @FxArg("email") String email) {}}
 *
 * v1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface FxArg {

    /** Identificador único do parâmetro no contexto da ação */
    String value();
}