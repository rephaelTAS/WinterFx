// NewSceneHandler.java v4.0 - 2026-08-23
// Handler para @NewScene com troca de cena e execução condicional AFTER.
// DESACOPLADO: Usa ResourceModule (Fachada) em vez de ResourceRegistry.
package com.ossobo.winterfx.view.handler;

import com.ossobo.winterfx.resources.ResourceModule;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;
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
import java.util.Objects;

/**
 * Handler para {@code @NewScene} — troca de cena após sucesso do método.
 *
 * <p>Dependências injetadas via construtor — NÃO acessa {@code WinterApplication}.</p>
 *
 * @version 4.0 (23/08/2026)
 */
public class NewSceneHandler implements AnnotationHandler<NewScene> {

    private static final System.Logger LOGGER = System.getLogger(NewSceneHandler.class.getName());

    private final StageManager stageManager;
    private final ResourceModule resourceModule;

    public NewSceneHandler(StageManager stageManager, ResourceModule resourceModule) {
        this.stageManager = Objects.requireNonNull(stageManager);
        this.resourceModule = Objects.requireNonNull(resourceModule);
    }

    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof NewScene;
    }

    @Override
    public Class<NewScene> getAnnotationType() {
        return NewScene.class;
    }

    /**
     * Executa a troca de cena após o sucesso do método anotado.
     * Usa ResourceModule.requireView() para obter o ViewDescriptor.
     */
    @Override
    public void handle(AnnotationContext context, NewScene annotation) {
        try {
            if (stageManager == null || resourceModule == null) return;

            // ✅ Usa ResourceModule.requireView() - Fail-Fast e O(1)
            ViewDescriptor descriptor = resourceModule.requireView(annotation.view());

            LoadedView<?> loadedView = stageManager.loadView(annotation.view());
            Parent root = loadedView.root();

            double width = annotation.width() > 0 ? annotation.width() : descriptor.width();
            double height = annotation.height() > 0 ? annotation.height() : descriptor.height();

            Scene newScene = new Scene(root, width, height);

            URL primaryCss = descriptor.primaryCss();
            if (primaryCss != null) newScene.getStylesheets().add(primaryCss.toExternalForm());

            List<URL> additionalCss = descriptor.additionalCss();
            if (additionalCss != null) {
                for (URL css : additionalCss) newScene.getStylesheets().add(css.toExternalForm());
            }

            Stage stage = stageManager.getPrimaryStage();
            if (stage == null) stage = new Stage();

            final Stage finalStage = stage;
            final String title = annotation.title().isEmpty() ? descriptor.title() : annotation.title();
            final boolean centered = annotation.centered();

            Platform.runLater(() -> {
                finalStage.setScene(newScene);
                finalStage.setTitle(title);
                if (centered) finalStage.centerOnScreen();
                finalStage.show();
            });

            LOGGER.log(System.Logger.Level.INFO, "Nova cena exibida: {0}", annotation.view());

        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.ERROR,
                    "Erro ao trocar para nova cena: " + annotation.view(), e);
        }
    }

    @Override public boolean isBeforePhase() { return false; }
    @Override public boolean isAfterPhase() { return true; }
    @Override public boolean isSuccessOnly() { return true; }
    @Override public boolean isErrorOnly() { return false; }
}