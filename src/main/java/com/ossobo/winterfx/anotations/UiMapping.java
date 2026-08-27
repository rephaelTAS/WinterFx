package com.ossobo.winterfx.anotations;

import java.lang.annotation.*;

/**
 * Rota de TRANSPORTE DE COMPONENTES VISUAIS. Namespace UI — livre para
 * compartilhar rota com qualquer outra anotação.
 *
 * <p>Não é para abrir telas: é o canal oficial para ENVIAR elementos FXML
 * (Label, TableView, TextField, Pane...) de um ponto para outro do app.</p>
 *
 * <pre>{@code
 * // Quem POSSUI o elemento envia:
 * Rotas.ui("painel/atualizar", Params.with("tabela", minhaTableView));
 *
 * // Quem RECEBE declara a intenção com @UI:
 * @UiMapping("atualizar")
 * public ResponseData atualizar(@UI("tabela") TableView<Livro> tabela) { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface UiMapping {
    String value() default "";
}