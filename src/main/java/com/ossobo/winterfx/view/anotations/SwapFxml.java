package com.ossobo.winterfx.view.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @SwapFxml v1.0
 *
 * Troca o conteúdo de um container (StackPane, BorderPane, etc.)
 * por um FXML carregado, antes ou depois da execução do método.
 *
 * Uso:
 * @SwapFxml(container = "areaConteudo", viewId = "detalhes")
 * private void onDetalhes() { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwapFxml {

    /** Nome do campo container (StackPane, Pane, etc.) */
    String container();

    /** ID da view registrada (@RegisterView) */
    String viewId();

    /** true = antes do método, false = depois */
    boolean before() default true;
}