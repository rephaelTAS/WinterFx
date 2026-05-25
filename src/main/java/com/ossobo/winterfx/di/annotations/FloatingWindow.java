package com.ossobo.winterfx.di.annotations;

import com.ossobo.winterfx.resources.enums.Modality;

import java.lang.annotation.*;

/**
 * Anotação para campos que abrem janelas flutuantes.
 *
 * <p>O FloatingWindowManager processa esta anotação e configura
 * o Stage automaticamente.</p>
 *
 * <pre>
 * // Janela simples (não bloqueante, singleton)
 * {@code @FloatingWindow(viewId = "livros")}
 * private Stage janelaLivros;
 *
 * // Janela modal bloqueante, múltiplas instâncias
 * {@code @FloatingWindow(viewId = "detalhes", modality = Modality.WINDOW_MODAL, multipleInstances = true)}
 * private Stage janelaDetalhes;
 *
 * // Auto-open (abre sozinha)
 * {@code @FloatingWindow(viewId = "detalhes", autoOpen = true)}
 * private Stage janelaDetalhes;
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Documented
public @interface FloatingWindow {

    /** ID da view registrada no ResourceRegistry */
    String viewId();

    /** Título da janela (vazio = usa o do descriptor) */
    String title() default "";

    /** Modalidade da janela (WINDOW_MODAL = bloqueia a janela pai) */
    Modality modality() default Modality.WINDOW_MODAL;

    /** Se true, apenas uma instância por vez (traz para frente se já aberta) */
    boolean singleton() default true;

    /** Se true, permite múltiplas instâncias simultâneas */
    boolean multipleInstances() default false;

    /** Se true, fecha automaticamente ao perder foco */
    boolean autoClose() default false;

    /** Se true, abre automaticamente após configuração */
    boolean autoOpen() default false;

    // ===== DIMENSÕES =====
    /** Largura da janela (0 = usa a do descriptor) */
    int width() default 0;

    /** Altura da janela (0 = usa a do descriptor) */
    int height() default 0;

    // ===== PROPRIEDADES =====
    /** Se true, a janela pode ser redimensionada */
    boolean resizable() default true;

    /** Se true, a janela fica sempre no topo */
    boolean alwaysOnTop() default false;
    // Em FloatingWindow.java:
    /** Se true, sempre carrega uma nova instância (dados frescos) */
    boolean fresh() default true;

    /** ID da janela owner (vazio = usa a janela ativa como owner) */
    String owner() default "";
}