package com.ossobo.winterfx.di.annotations;

import com.ossobo.winterfx.resources.descriptor.ViewDescriptor.Modality;

import java.lang.annotation.*;

/**
 * Anotação para métodos que abrem janelas flutuantes.
 *
 * <p>O FloatingWindowManager processa esta anotação e abre
 * a janela automaticamente quando o método é chamado.</p>
 *
 * <pre>
 * {@code
 * @FloatingWindow(
 *     viewId = "detalhes-usuario",
 *     modality = Modality.WINDOW_MODAL,
 *     title = "Detalhes do Usuário",
 *     singleton = true
 * )
 * private void abrirDetalhes(Usuario usuario) {
 *     // Este código roda DEPOIS que a janela foi configurada
 *     // O parâmetro 'usuario' é passado para o configurador
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface FloatingWindow {

    /** ID da view registrada */
    String viewId();

    /** Título da janela (vazio = usa o do descriptor) */
    String title() default "";

    /** Modalidade da janela */
    Modality modality() default Modality.WINDOW_MODAL;

    /** Se true, apenas uma instância por vez */
    boolean singleton() default true;

    /** Se true, permite múltiplas instâncias */
    boolean multipleInstances() default false;

    /** Se true, fecha automaticamente ao perder foco */
    boolean autoClose() default false;
}