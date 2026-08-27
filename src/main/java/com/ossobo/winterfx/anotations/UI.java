package com.ossobo.winterfx.anotations;

import java.lang.annotation.*;

/**
 * Injeta um componente visual JavaFX (Label, TableView, TextField, Pane...)
 * em um parâmetro de handler, extraindo-o do mapa de parâmetros despachado.
 *
 * <p><b>NÃO confundir com</b> {@link UiMapping}: aquela marca rotas no nível de
 * MÉTODO (canal UI); esta marca injeção no nível de PARÂMETRO. Elas trabalham juntas:</p>
 *
 * <pre>{@code
 * @UiMapping("enviar-selecao")                          // rota
 * public ResponseData enviarSelecao(@UI("tabela") TableView<Livro> t) { ... }
 *                      //              ↑ injeção do componente
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UI {
    /** Identificador do componente no mapa de parâmetros. */
    String value();
}