package com.ossobo.winterfx.runtime.handler.after;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.anotations.NewScene;
import com.ossobo.winterfx.view.loader.LoadedView;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;

/** Handler para @NewScene — executado DEPOIS do método (sucesso). */
public class NewSceneHandler implements AnnotationHandler<NewScene> {

    @Override
    public Class<NewScene> getAnnotationType() {
        return NewScene.class;
    }

    @Override
    public void handle(AnnotationContext context, NewScene annotation) {
        try {
            WinterApplication winter = WinterApplication.getInstance();
            if (winter == null) return;

            StageManager stageManager = winter.getStageManager();
            ResourceRegistry registry = winter.getResourceRegistry();
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

            Stage stage = winter.getPrimaryStage();
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
}