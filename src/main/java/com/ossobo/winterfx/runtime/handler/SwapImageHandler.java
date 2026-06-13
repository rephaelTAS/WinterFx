package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.imagemanager.ImageManager;
import com.ossobo.winterfx.imagemanager.anotations.SwapImage;
import javafx.application.Platform;
import javafx.scene.image.ImageView;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SwapImageHandler implements AnnotationHandler<SwapImage> {

    private static final Logger LOGGER = Logger.getLogger(SwapImageHandler.class.getName());
    private final ImageManager imageManager;

    public SwapImageHandler(ImageManager imageManager) {
        this.imageManager = imageManager;
    }

    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof SwapImage;
    }

    @Override
    public Class<SwapImage> getAnnotationType() {
        return SwapImage.class;
    }

    @Override
    public void handle(AnnotationContext ctx, SwapImage ann) {
        Platform.runLater(() -> {
            try {
                Object target = ctx.getTarget();
                // Busca o campo na hierarquia de classes (proxy herda do original)
                Field field = findField(target.getClass(), ann.imageView());
                if (field == null) {
                    LOGGER.warning("Campo ImageView não encontrado: " + ann.imageView());
                    return;
                }
                field.setAccessible(true);
                Object value = field.get(target);

                if (value instanceof ImageView imageView) {
                    if (ann.width() > 0 && ann.height() > 0) {
                        imageManager.load(imageView, ann.imageId(), ann.width(), ann.height());
                    } else {
                        imageManager.load(imageView, ann.imageId());
                    }
                } else {
                    LOGGER.warning("Campo '" + ann.imageView() + "' não é ImageView: " +
                            (value != null ? value.getClass().getName() : "null"));
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Erro ao processar @SwapImage: " + ann.imageView(), e);
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