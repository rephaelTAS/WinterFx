package com.ossobo.winterfx.di.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🖼️ @FXImage - Anotação para injeção automática de imagens em ImageView
 *
 * Uso:
 *   @FXML
 *   @FXImage(AppImageConfig.System.MENU)
 *   private ImageView catalogoIcon;
 *
 * @author Rafael Tavares
 * @since 1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FXImage {

    /**
     * ID da imagem registrada no ResourceAPI
     */
    String value();

    /**
     * Largura desejada (0 = manter original)
     */
    double width() default 0;

    /**
     * Altura desejada (0 = manter original)
     */
    double height() default 0;

    /**
     * Manter proporção ao redimensionar
     */
    boolean preserveRatio() default true;

    /**
     * Usar suavização (smooth)
     */
    boolean smooth() default true;
}