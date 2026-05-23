package com.ossobo.winterfx.di.annotations;

package com.desktopspring.annotations;

import java.lang.annotation.*;

/**
 * Injeta uma view registrada em um campo.
 *
 * A view é carregada a partir do ViewRegistry usando o ID especificado.
 * O campo deve ser do tipo Parent, Pane, ou qualquer Node do JavaFX.
 *
 * Exemplos de uso:
 *
 * <pre>
 * {@code
 * // Injeta view em um StackPane (substitui conteúdo)
 * @InjectView("usuarios")
 * private StackPane painelCentral;
 *
 * // Injeta view em um BorderPane (área central)
 * @InjectView("dashboard")
 * private BorderPane areaPrincipal;
 *
 * // Abre view em nova janela
 * @InjectView(value = "configuracoes", newStage = true, title = "Configurações")
 * private Parent janelaConfig;
 *
 * // Injeta apenas um child específico do FXML
 * @InjectView(value = "modulo", child = "tabelaDados")
 * private VBox apenasTabela;
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface InjectView {

    /**
     * ID da view registrada (obrigatório).
     * Deve corresponder ao ID usado em @RegisterView.
     */
    String value();

    /**
     * Se true, abre a view em uma nova janela (Stage).
     * Se false (padrão), injeta no painel/container.
     */
    boolean newStage() default false;

    /**
     * Título da nova janela (se newStage = true).
     * Se vazio, usa o título definido no @RegisterView.
     */
    String title() default "";

    /**
     * Se true, a view só é carregada quando acessada pela primeira vez.
     * Útil para views que não são usadas imediatamente.
     */
    boolean lazy() default false;

    /**
     * ID de um nó filho específico do FXML a ser extraído.
     * Se vazio, o nó raiz do FXML é usado.
     *
     * Exemplo: Se o FXML tem <VBox fx:id="tabelaDados">,
     * use child = "tabelaDados" para injetar apenas esse VBox.
     */
    String child() default "";

    /**
     * Animação ao carregar a view.
     */
    ViewAnimation animation() default ViewAnimation.NONE;

    /**
     * Duração da animação em milissegundos.
     */
    int animationDuration() default 300;

    /**
     * Se true, mantém a view em cache para reutilização.
     * Se false, recarrega o FXML a cada acesso.
     */
    boolean cache() default true;

    /**
     * Se true, carrega a view em uma thread separada.
     * Útil para views pesadas que não devem travar a UI.
     */
    boolean async() default false;

    /**
     * Texto/mensagem exibida enquanto a view carrega (modo async).
     */
    String loadingMessage() default "Carregando...";

    /**
     * Se true, lança exceção se a view não for encontrada.
     * Se false, apenas loga um aviso.
     */
    boolean required() default true;
}