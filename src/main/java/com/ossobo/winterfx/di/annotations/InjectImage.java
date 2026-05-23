package com.ossobo.winterfx.di.annotations;

import java.lang.annotation.*;

/**
 * Injeta uma imagem registrada em um campo JavaFX.
 *
 * <p>A imagem é localizada no {@code ImageRegistry} pelo {@link #value() ID}
 * e injetada no campo anotado.</p>
 *
 * <p>Tipos de campo suportados:</p>
 * <ul>
 *   <li>{@code javafx.scene.image.Image} - Objeto Image do JavaFX</li>
 *   <li>{@code javafx.scene.image.ImageView} - ImageView configurado</li>
 *   <li>{@code javafx.scene.layout.Background} - Background para panes</li>
 * </ul>
 *
 * <h3>Exemplos:</h3>
 * <pre>
 * {@code
 * // Injeta como Image
 * @InjectImage("logo")
 * private Image logoImage;
 *
 * // Injeta como ImageView com dimensões
 * @InjectImage(value = "logo", width = 200, height = 80)
 * private ImageView logoView;
 *
 * // Injeta como Background (para panes)
 * @InjectImage(value = "bg-main", asBackground = true)
 * private Background fundo;
 *
 * // Injeta como ícone
 * @InjectImage(value = "icon-save", imageType = ImageType.ICON, width = 16, height = 16)
 * private ImageView iconSave;
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface InjectImage {

    /**
     * ID da imagem registrada (obrigatório).
     * Deve corresponder ao {@code id} usado em {@code @RegisterImage}.
     */
    String value();

    /**
     * Largura desejada para a imagem/ImageView.
     * Sobrescreve o preferredWidth do registro.
     * -1 = usar o valor do registro ou tamanho original.
     */
    double width() default -1;

    /**
     * Altura desejada para a imagem/ImageView.
     * Sobrescreve o preferredHeight do registro.
     * -1 = usar o valor do registro ou tamanho original.
     */
    double height() default -1;

    /**
     * Se deve preservar a proporção ao redimensionar.
     * Sobrescreve o preserveRatio do registro.
     */
    boolean preserveRatio() default true;

    /**
     * Se deve aplicar suavização (smooth).
     * Sobrescreve o smooth do registro.
     */
    boolean smooth() default true;

    /**
     * Se true, injeta como {@code javafx.scene.layout.Background}
     * em vez de Image/ImageView.
     *
     * <p>Útil para definir fundo de panes.</p>
     */
    boolean asBackground() default false;

    /**
     * Se true, lança exceção se a imagem não for encontrada.
     * Se false, apenas loga um aviso.
     */
    boolean required() default true;

    /**
     * Se true, carrega a imagem em background.
     * Útil para imagens grandes.
     */
    boolean async() default false;

    /**
     * Se true, mantém a imagem em cache para reutilização.
     */
    boolean cache() default true;
}