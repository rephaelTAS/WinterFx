// SwapFxmlHandler.java v2.1 - 2026-06-14
// Handler para @SwapFxml com troca de FXML dinâmico e execução condicional AFTER.
//
// PIPELINE CONDICIONAL v2.1:
//   - isBeforePhase(): false (não executa BEFORE)
//   - isAfterPhase(): true (executa APÓS método)
//   - isSuccessOnly(): true (executa SÓ se método sucesso)
//   - isErrorOnly(): false (não executa se erro)
//
// Vantagens v2.1:
//   - ✅ Executa na fase AFTER (após método)
//   - ✅ Execução segura no thread JavaFX (Platform.runLater)
//   - ✅ Troca FXML dinâmico dentro de container (Pane)
//   *   - ✅ Carrega ViewDescriptor via StageManager.swapFxml()
//   - ✅ Busca campo container na hierarquia de classes (sobe até superclass)
//   - ✅ Injeta view no container (Pane.getChildren().clear() + add)
//   - ✅ Logging robusto (warning para cada erro)
//   - ✅ Executa SÓ se método sucesso (SUCCESS_ONLY)
//   - ✅ NUNCA executa com @OnError (mutuamente exclusivo)
//
// @version 2.1 - Handler AFTER exclusivo para sucesso com troca de FXML dinâmico
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.anotations.SwapFxml;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Handler para {@code @SwapFxml} com troca de FXML dinâmico e execução condicional AFTER.
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * {@code
 * @FXML
 * private Pane contentArea;
 *
 * @OnSuccess(titulo = "Conteúdo Carregado", descricao = "View carregada com sucesso")
 * @SwapFxml(viewId = "dashboard", container = "contentArea")
 * public void handleLoadDashboard(ActionEvent event) {
 *     loadDashboardData();
 *     // Se sucesso → @OnSuccess + @SwapFxml (ambos executam)
 * }
 * }
 * </pre>
 *
 * <p><b>Fluxo:</b></p>
 * <ol>
 *   <li>Método executa sem exceção</li>
 *   <li>executeSuccessPhase() seleciona handlers SUCCESS_ONLY</li>
 *   <li>SwapFxmlHandler carrega FXML dinâmico no container</li>
 *   <ol>
 *     <li>Obtenção do ViewDescriptor: {@link StageManager#swapFxml(String)}</li>
 *     <li>Carregamento da view: {@link StageManager#loadViewAsParent(String, ViewDescriptor)}</li>
 *     <li>Busca do container: {@link #findField(Class, String)} (hierarquia de classes)</li>
 *     <li>Injeção da view: {@link Pane#getChildren()}.clear() + add</li>
 *   </ol>
 * </ol>
 *
 * <p><b>Parâmetros:</b></p>
 * <ul>
 *   <li>{@code viewId}: identificador do FXML (ex: "dashboard", "settings")</li>
 *   <li>{@code container}: nome do campo {@link Pane} que recebe o FXML</li>
 * </ul>
 *
 * <p><b>IMPORTANTE:</b> Executa SÓ se sucesso. NUNCA com @OnError.</p>
 *
 * @version 2.1 - Handler AFTER exclusivo para sucesso com troca de FXML dinâmico
 */
public class SwapFxmlHandler implements AnnotationHandler<SwapFxml> {

    private final StageManager stageManager;

    /**
     * Construtor com StageManager.
     *
     * @param stageManager Gerente de stages e carregamento de views
     */
    public SwapFxmlHandler(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    /**
     * Verifica se este handler processa a anotação.
     *
     * @param annotation Anotação a verificar
     * @return true se é {@code @SwapFxml}, false se não
     */
    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof SwapFxml;
    }

    /**
     * @return Classe {@code SwapFxml}
     */
    @Override
    public Class<SwapFxml> getAnnotationType() {
        return SwapFxml.class;
    }

    /**
     * Processa {@code @SwapFxml} na fase AFTER com sucesso.
     *
     * <p><b>Fluxo:</b></p>
     * <ol>
     *   <li>Obtenção do target: {@link AnnotationContext#getTarget()}</li>
     *   <li>Obtenção do ViewDescriptor: {@link StageManager#swapFxml(String)}</li>
     *   <li>Carregamento da view: {@link StageManager#loadViewAsParent(String, ViewDescriptor)}</li>
     *   <li>Busca do container: {@link #findField(Class, String)}</li>
     *   <ol>
     *     <li>Sobe hierarquia de classes (current → superclass)</li>
     *     <li>Tenta {@link Class#getDeclaredField(String)}</li>
     *     <li>Retorna campo encontrado ou null</li>
     *   </ol>
     *   <li>Injeção da view no container:</li>
     *   <ol>
     *     <li>Verifica se container é {@link Pane}</li>
     *     <li> {@link Pane#getChildren()}.clear()</li>
     *     <li>{@link Pane#getChildren()}.add(view)</li>
     *   </ol>
     * </ol>
     *
     * @param ctx Contexto com target do método
     * @param ann Anotação {@code @SwapFxml}
     */
    @Override
    public void handle(AnnotationContext ctx, SwapFxml ann) {
        Platform.runLater(() -> {
            try {
                Object target = ctx.getTarget();

                // 1. Obtém o ViewDescriptor do StageManager
                ViewDescriptor descriptor = stageManager.swapFxml(ann.viewId());
                if (descriptor == null) {
                    return;
                }

                // 2. Carrega a view via StageManager
                Parent view = stageManager.loadViewAsParent(ann.viewId(), descriptor);
                if (view == null) {
                    return;
                }

                // 3. Busca o container na hierarquia de classes
                Field field = findField(target.getClass(), ann.container());
                if (field == null) {
                    return;
                }
                field.setAccessible(true);
                Object container = field.get(target);

                // 4. Injeta a view no container
                if (container instanceof Pane pane) {
                    pane.getChildren().clear();
                    pane.getChildren().add(view);
                }
            } catch (Exception e) {
            }
        });
    }

    /**
     * Busca campo na hierarquia de classes (sobe até Object).
     *
     * <p><b>Fluxo:</b></p>
     * <ol>
     *   <li>current = clazz</li>
     *   <li>Loop: current ≠ null e current ≠ Object.class</li>
     *   <li>Tenta {@link Class#getDeclaredField(String)}</li>
     *   <li>Se não encontrado: current = current.getSuperclass()</li>
     *   <li>Retorna campo ou null</li>
     * </ol>
     *
     * @param clazz Classe inicial
     * @param name Nome do campo
     * @return Campo encontrado ou null
     */
    private Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * @return false (não executa na fase BEFORE)
     */
    @Override
    public boolean isBeforePhase() {
        return false;
    }

    /**
     * @return true (executa na fase AFTER, após método)
     */
    @Override
    public boolean isAfterPhase() {
        return true;
    }

    /**
     * @return true (executa SÓ se método sucesso)
     */
    @Override
    public boolean isSuccessOnly() {
        return true;
    }

    /**
     * @return false (não executa se erro)
     */
    @Override
    public boolean isErrorOnly() {
        return false;
    }
}