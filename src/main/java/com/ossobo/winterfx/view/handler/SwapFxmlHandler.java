// SwapFxmlHandler.java v3.0 - 2026-07-01
// Handler para @SwapFxml com troca de FXML dinâmico e execução condicional AFTER.
// DESACOPLADO: StageManager injetado via construtor.
package com.ossobo.winterfx.view.handler;

import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.anotations.SwapFxml;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Handler para {@code @SwapFxml} — troca de FXML dinâmico após sucesso do método.
 *
 * <p>StageManager injetado via construtor.</p>
 *
 * @version 3.0 (01/07/2026)
 */
public class SwapFxmlHandler implements AnnotationHandler<SwapFxml> {

    private final StageManager stageManager;

    public SwapFxmlHandler(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof SwapFxml;
    }

    @Override
    public Class<SwapFxml> getAnnotationType() {
        return SwapFxml.class;
    }

    @Override
    public void handle(AnnotationContext ctx, SwapFxml ann) {
        Platform.runLater(() -> {
            try {
                Object target = ctx.getTarget();

                ViewDescriptor descriptor = stageManager.swapFxml(ann.viewId());
                if (descriptor == null) return;

                Parent view = stageManager.loadViewAsParent(ann.viewId(), descriptor);
                if (view == null) return;

                Field field = findField(target.getClass(), ann.container());
                if (field == null) return;

                field.setAccessible(true);
                Object container = field.get(target);

                if (container instanceof Pane pane) {
                    pane.getChildren().clear();
                    pane.getChildren().add(view);
                }
            } catch (Exception ignored) {}
        });
    }

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

    @Override public boolean isBeforePhase() { return false; }
    @Override public boolean isAfterPhase() { return true; }
    @Override public boolean isSuccessOnly() { return true; }
    @Override public boolean isErrorOnly() { return false; }
}