package com.ossobo.winterfx.view.anotations;

import java.lang.annotation.*;

/**
 * Vincula um método do controller a um botão FXML pelo fx:id.
 *
 * <p>Se não especificado, usa o nome do método como fx:id.</p>
 *
 * <pre>{@code
 * @FXMLAction("btnSalvar")
 * @OnSuccess("Salvo com sucesso!")
 * public void salvar() { }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FXMLAction {
    /** fx:id do botão no FXML */
    String value();
}