// NewSceneHandler.java v3.0 - 2026-07-01
// Handler para @NewScene com troca de cena e execução condicional AFTER.
// DESACOPLADO: StageManager injetado, sem dependência do WinterApplication.
package com.ossobo.winterfx.view.handler;

import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;
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
 * Handler para {@code @NewScene} — troca de cena após sucesso do método.
 *
 * <p>Dependências injetadas via construtor — NÃO acessa {@code WinterApplication}.</p>
 *
 * @version 3.0 (01/07/2026)
 */
public class NewSceneHandler implements AnnotationHandler<NewScene> {

    private final StageManager stageManager;
    private final ResourceRegistry registry;

    public NewSceneHandler(StageManager stageManager, ResourceRegistry registry) {
        this.stageManager = stageManager;
        this.registry = registry;
    }

    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof NewScene;
    }

    @Override
    public Class<NewScene> getAnnotationType() {
        return NewScene.class;
    }

    @Override
    public void handle(AnnotationContext context, NewScene annotation) {
        try {
            if (stageManager == null || registry == null) return;

            LoadedView<?> loadedView = stageManager.loadView(annotation.view());
            Parent root = loadedView.getRoot();

            ViewDescriptor descriptor = registry.findViewById(annotation.view()).orElse(null);
            if (descriptor == null) return;

            double width = annotation.width() > 0 ? annotation.width() : descriptor.getWidth();
            double height = annotation.height() > 0 ? annotation.height() : descriptor.getHeight();

            Scene newScene = new Scene(root, width, height);

            URL primaryCss = descriptor.getPrimaryCss();
            if (primaryCss != null) newScene.getStylesheets().add(primaryCss.toExternalForm());

            List<URL> additionalCss = descriptor.getAdditionalCss();
            if (additionalCss != null) {
                for (URL css : additionalCss) newScene.getStylesheets().add(css.toExternalForm());
            }

            Stage stage = stageManager.getPrimaryStage();
            if (stage == null) stage = new Stage();

            final Stage finalStage = stage;
            final String title = annotation.title().isEmpty() ? descriptor.getTitle() : annotation.title();
            final boolean centered = annotation.centered();

            Platform.runLater(() -> {
                finalStage.setScene(newScene);
                finalStage.setTitle(title);
                if (centered) finalStage.centerOnScreen();
                finalStage.show();
            });

        } catch (Exception ignored) {}
    }

    @Override public boolean isBeforePhase() { return false; }
    @Override public boolean isAfterPhase() { return true; }
    @Override public boolean isSuccessOnly() { return true; }
    @Override public boolean isErrorOnly() { return false; }
}