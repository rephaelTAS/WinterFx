package com.ossobo.winterfx.view.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injeta um controller APÓS o FXML ter sido carregado.
 * Diferente de @Inject, garante que @FXML já estejam disponíveis.
 *
 * Uso:
 * @GetController
 * private CatalogoListController catalogoController;
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GetController {
}