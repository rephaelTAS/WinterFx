package com.ossobo.winterfx.imagemanager.anotations;


import com.ossobo.winterfx.imagemanager.enums.ImageType;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;

import java.lang.annotation.*;

/**
 * Anotação para registro automático de imagens no WinterFX.
 *
 * <p>Segue o mesmo padrão do {@code @RegisterView}: trabalha com
 * caminhos {@code String} que são resolvidos para {@link java.net.URL}
 * via classpath.</p>
 *
 * <p>Basta anotar qualquer classe ou usar via configuração para
 * registrar imagens que serão injetadas com {@code @InjectImage}.</p>
 *
 * <H3>Exemplo de uso:</h3>
 * <pre>
 * {@code
 * @RegisterImage(
 *     id = "logo",
 *     src = "/images/logo.png",
 *     imageType = ImageType.IMAGE,
 *     preferredWidth = 200,
 *     preferredHeight = 80,
 *     preserveRatio = true
 * )
 *
 * // Ou registrando múltiplas imagens em uma classe
 * @RegisterImages({
 *     @RegisterImage(id = "logo", src = "/images/logo.png"),
 *     @RegisterImage(id = "icon-user", src = "/icons/user.png", imageType = ImageType.ICON),
 *     @RegisterImage(id = "bg-main", src = "/images/bg.png", imageType = ImageType.BACKGROUND)
 * })
 * }
 * </pre>
 *
 * @see com.ossobo.winterfx.resources.descriptor.ImageDescriptor
 * @see InjectImage
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
@Documented
@Repeatable(RegisterImages.class)
public @interface RegisterImage {

    // =============================================
    // IDENTIFICAÇÃO
    // =============================================

    /**
     * ID único da imagem (obrigatório).
     *
     * <p>Este ID é usado como chave no {@code ImageRegistry} e
     * referenciado por {@code @InjectImage}.</p>
     */
    String id();

    /**
     * Descrição opcional para documentação.
     */
    String description() default "";

    /**
     * Tags para categorização e busca.
     * Ex: {"logo", "branding", "header"}
     */
    String[] tags() default {};

    // =============================================
    // FONTE
    // =============================================

    /**
     * Caminho da imagem (obrigatório).
     *
     * <p>Relativo ao classpath. Será resolvido para {@link java.net.URL}.
     * Ex: "/images/logo.png", "/icons/user.png"</p>
     */
    String src();

    /**
     * Origem do recurso.
     * Padrão: {@link ResourceOrigin#APPLICATION}
     */
    ResourceOrigin origin() default ResourceOrigin.APPLICATION;

    // =============================================
    // TIPO
    // =============================================

    /**
     * Tipo da imagem.
     *
     * <ul>
     *   <li>{@link ImageType#IMAGE IMAGE}: Imagem normal</li>
     *   <li>{@link ImageType#ICON ICON}: Ícone</li>
     *   <li>{@link ImageType#BACKGROUND BACKGROUND}: Fundo</li>
     *   <li>{@link ImageType#SPRITE SPRITE}: Sprite sheet</li>
     * </ul>
     */
    ImageType imageType() default ImageType.IMAGE;

    // =============================================
    // DIMENSÕES
    // =============================================

    /**
     * Largura preferida da imagem.
     * -1 = usar tamanho original.
     */
    double preferredWidth() default -1;

    /**
     * Altura preferida da imagem.
     * -1 = usar tamanho original.
     */
    double preferredHeight() default -1;

    /**
     * Se deve preservar a proporção ao redimensionar.
     */
    boolean preserveRatio() default true;

    /**
     * Se deve aplicar suavização (smooth) ao redimensionar.
     */
    boolean smooth() default true;
}