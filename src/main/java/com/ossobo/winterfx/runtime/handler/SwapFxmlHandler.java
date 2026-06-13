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
import java.util.logging.Level;
import java.util.logging.Logger;

public class SwapFxmlHandler implements AnnotationHandler<SwapFxml> {

    private static final Logger LOGGER = Logger.getLogger(SwapFxmlHandler.class.getName());
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

                // 1. Obtém o ViewDescriptor do ResourceRegistry
                ViewDescriptor descriptor = stageManager.swapFxml(ann.viewId());
                if (descriptor == null) {
                    LOGGER.warning("ViewDescriptor não encontrado: " + ann.viewId());
                    return;
                }

                // 2. Carrega a view via StageManager
                Parent view = stageManager.loadViewAsParent(ann.viewId(), descriptor);
                if (view == null) {
                    LOGGER.warning("Falha ao carregar view: " + ann.viewId());
                    return;
                }

                // 3. Busca o container na hierarquia de classes
                Field field = findField(target.getClass(), ann.container());
                if (field == null) {
                    LOGGER.warning("Container não encontrado: " + ann.container());
                    return;
                }
                field.setAccessible(true);
                Object container = field.get(target);

                // 4. Injeta a view no container
                if (container instanceof Pane pane) {
                    pane.getChildren().clear();
                    pane.getChildren().add(view);
                } else {
                    LOGGER.warning("Campo '" + ann.container() + "' não é Pane: " +
                            (container != null ? container.getClass().getName() : "null"));
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Erro ao processar @SwapFxml: " + ann.viewId(), e);
            }
        });
    }

    /** Busca campo na hierarquia de classes (sobe até Object) */
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
}