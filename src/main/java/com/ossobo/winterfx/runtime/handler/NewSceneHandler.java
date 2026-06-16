// NewSceneHandler.java v2.1 - 2026-06-14
// Handler para @NewScene com troca de cena e execução condicional AFTER.
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
//   - ✅ Troca cena com FXML carregado dinamicamente
//   - ✅ Aplica CSS principal + adicionais do descriptor
//   - ✅ Define tamanho da cena (annotation ou descriptor)
//   - ✅ Define título da stage (annotation ou descriptor)
//   - ✅ Centraliza stage opcionalmente
//   - ✅ Executa SÓ se método sucesso (SUCCESS_ONLY)
//   - ✅ NUNCA executa com @OnError (mutuamente exclusivo)
//   - ✅ Try-catch robusto (não loga erro, ignora silenciosamente)
//
// @version 2.1 - Handler AFTER exclusivo para sucesso com troca de cena
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.anotations.NewScene;
import com.ossobo.winterfx.view.loader.LoadedView;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.List;

/**
 * Handler para {@code @NewScene} com troca de cena e execução condicional AFTER.
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * {@code
 * @OnSuccess(titulo = "Login Confirmado", descricao = "Bem-vindo!")
 * @NewScene(view = "main", width = 800, height = 600, title = "Main App", centered = true)
 * public void handleLogin(ActionEvent event) {
 *     authService.login(username, password);
 *     // Se sucesso → @OnSuccess + @NewScene (ambos executam)
 * }
 * }
 * </pre>
 *
 * <p><b>Fluxo:</b></p>
 * <ol>
 *   <li>Método executa sem exceção</li>
 *   <li>executeSuccessPhase() seleciona handlers SUCCESS_ONLY</li>
 *   <li>NewSceneHandler carrega FXML e cria nova cena</li>
 *   <li>Aplica CSS do descriptor (principal + adicionais)</li>
 *   <li>Troca cena na stage e centraliza (opcional)</li>
 * </ol>
 *
 * <p><b>Parâmetros:</b></p>
 * <ul>
 *   <li>{@code view}: nome do FXML (ex: "main", "login")</li>
 *   <li>{@code width}: largura da cena (0 = usa descriptor)</li>
 *   <li>{@code height}: altura da cena (0 = usa descriptor)</li>
 *   <li>{@code title}: título da stage (empty = usa descriptor)</li>
 *   <li>{@code centered}: centraliza stage na tela</li>
 * </ul>
 *
 * <p><b>IMPORTANTE:</b> Executa SÓ se sucesso. NUNCA com @OnError.</p>
 *
 * @version 2.1 - Handler AFTER exclusivo para sucesso com troca de cena
 */
public class NewSceneHandler implements AnnotationHandler<NewScene> {

    /**
     * Verifica se este handler processa a anotação.
     *
     * @param annotation Anotação a verificar
     * @return true se é {@code @NewScene}, false se não
     */
    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof NewScene;
    }

    /**
     * @return Classe {@code NewScene}
     */
    @Override
    public Class<NewScene> getAnnotationType() {
        return NewScene.class;
    }

    /**
     * Processa {@code @NewScene} na fase AFTER com sucesso.
     *
     * <p><b>Fluxo:</b></p>
     * <ol>
     *   <li>Obtenção de instâncias: {@link WinterApplication}, {@link StageManager}, {@link ResourceRegistry}</li>
     *   <li>Carregamento do FXML: {@link StageManager#loadView(String)}</li>
     *   <li>Obtenção do descriptor: {@link ResourceRegistry#findViewById(String)}</li>
     *   <li>Cálculo de tamanho: annotation (se > 0) ou descriptor</li>
     *   <li>Criação da cena: {@link Scene#Scene(Parent, double, double)}</li>
     *   <li>Aplicação de CSS:</li>
     *   <ol>
     *     <li>CSS principal do descriptor</li>
     *     <li>CSS adicionais do descriptor</li>
     *   </ol>
     *   <li>Troca de cena na stage (Platform.runLater):</li>
     *   <ol>
     *     <li>{@link Stage#setScene(Scene)}</li>
     *     <li>{@link Stage#setTitle(String)}</li>
     *     <li>{@link Stage#centerOnScreen()} (opcional)</li>
     *     <li>{@link Stage#show()}</li>
     *   </ol>
     * </ol>
     *
     * @param context Contexto com resultado do método (não usado)
     * @param annotation Anotação {@code @NewScene}
     */
    @Override
    public void handle(AnnotationContext context, NewScene annotation) {
        try {
            // Obtenção da instância principal
            WinterApplication winter = WinterApplication.getInstance();
            if (winter == null) return;

            StageManager stageManager = winter.getStageManager();
            ResourceRegistry registry = winter.getResourceRegistry();
            if (stageManager == null || registry == null) return;

            // Carregamento do FXML
            LoadedView<?> loadedView = stageManager.loadView(annotation.view());
            Parent root = loadedView.getRoot();

            ViewDescriptor descriptor = registry.findViewById(annotation.view()).orElse(null);
            if (descriptor == null) return;

            // Cálculo de tamanho da cena
            double width = annotation.width() > 0 ? annotation.width() : descriptor.getWidth();
            double height = annotation.height() > 0 ? annotation.height() : descriptor.getHeight();

            // Criação da nova cena
            Scene newScene = new Scene(root, width, height);

            // Aplicação de CSS principal
            URL primaryCss = descriptor.getPrimaryCss();
            if (primaryCss != null) newScene.getStylesheets().add(primaryCss.toExternalForm());

            // Aplicação de CSS adicionais
            List<URL> additionalCss = descriptor.getAdditionalCss();
            if (additionalCss != null) {
                for (URL css : additionalCss) newScene.getStylesheets().add(css.toExternalForm());
            }

            // Obtenção da stage
            Stage stage = winter.getPrimaryStage();
            if (stage == null) stage = new Stage();

            // Configuração da stage
            final Stage finalStage = stage;
            final String title = annotation.title().isEmpty() ? descriptor.getTitle() : annotation.title();
            final boolean centered = annotation.centered();

            // Execução no thread JavaFX
            Platform.runLater(() -> {
                finalStage.setScene(newScene);
                finalStage.setTitle(title);
                if (centered) finalStage.centerOnScreen();
                finalStage.show();
            });

        } catch (Exception ignored) {
            // Ignora silenciosamente (não loga erro)
        }
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