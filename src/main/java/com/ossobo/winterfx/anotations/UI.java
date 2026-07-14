package com.ossobo.winterfx.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injeta um componente visual JavaFX (Label, Pane, TableView, etc).
 * Exemplo de uso no contexto: Params.with("statusLabel", meuLabel)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface UI {
    /** O identificador do componente JavaFX no mapa de parâmetros. */
    String value();
}