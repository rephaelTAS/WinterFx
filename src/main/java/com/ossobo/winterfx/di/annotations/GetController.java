package com.ossobo.winterfx.di.annotations;

package com.desktopspring.annotations;

import java.lang.annotation.*;

/**
 * Obtém o controller de uma view registrada e injeta no campo.
 *
 * O controller é obtido do ViewRegistry após a view ser carregada.
 * O campo deve ser do tipo da classe do controller.
 *
 * Exemplos de uso:
 *
 * <pre>
 * {@code
 * // Obtém controller pelo ID da view
 * @GetController("usuarios")
 * private UsuarioController usuarioController;
 *
 * // Controller opcional (não lança exceção se não encontrar)
 * @GetController(value = "debug", required = false)
 * private DebugController debugController;
 *
 * // Uso combinado com @InjectView
 * @InjectView("usuarios")
 * private StackPane painel;
 *
 * @GetController("usuarios")
 * private UsuarioController controller;
 *
 * // Agora pode usar o controller
 * controller.carregarDados();
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface GetController {

    /**
     * ID da view cujo controller será obtido (obrigatório).
     * Deve corresponder ao ID usado em @RegisterView.
     */
    String value();

    /**
     * Se true (padrão), lança exceção se o controller não for encontrado.
     * Se false, o campo permanece null.
     */
    boolean required() default true;

    /**
     * Se true, espera a view ser carregada antes de injetar o controller.
     * Útil quando a view é carregada de forma lazy ou async.
     */
    boolean waitForLoad() default true;
}