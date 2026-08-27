// SwapImageHandler.java v5.0 - 2026-08-22
// Handler com System.Logger
package com.ossobo.winterfx.imagemanager.handler;

import com.ossobo.winterfx.imagemanager.ImageManager;
import com.ossobo.winterfx.imagemanager.anotations.SwapImage;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;

import javafx.application.Platform;
import javafx.scene.image.ImageView;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Handler para {@code @SwapImage} com troca de imagem dinâmica.
 *
 * @version 5.0 (22/08/2026) - System.Logger + JPMS-Safe + Fail-Fast
 */
public class SwapImageHandler implements AnnotationHandler<SwapImage> {

    private static final System.Logger LOGGER = System.getLogger(SwapImageHandler.class.getName());

    private final ImageManager imageManager;

    public SwapImageHandler(ImageManager imageManager) {
        this.imageManager = Objects.requireNonNull(imageManager, "imageManager não pode ser null");
    }

    @Override
    public boolean supports(Annotation annotation) {
        return false;
    }

    @Override
    public Class<SwapImage> getAnnotationType() {
        return SwapImage.class;
    }

    @Override
    public void handle(AnnotationContext ctx, SwapImage annotation) {
        Platform.runLater(() -> {
            try {
                var target = ctx.target();
                if (target == null) {
                    throw new IllegalStateException("Target é null - não foi possível trocar imagem");
                }

                // Busca o campo na hierarquia de classes
                var field = findField(target.getClass(), annotation.imageView());
                if (field == null) {
                    throw new IllegalStateException(
                            String.format("Campo ImageView '%s' não encontrado em %s",
                                    annotation.imageView(), target.getClass().getName())
                    );
                }

                // JPMS-SAFE: Verifica antes de setAccessible
                if (!field.canAccess(target)) {
                    field.setAccessible(true);
                }

                var value = field.get(target);

                if (!(value instanceof ImageView imageView)) {
                    throw new IllegalStateException(
                            String.format("Campo '%s' não é um ImageView em %s (tipo: %s)",
                                    annotation.imageView(), target.getClass().getName(),
                                    value != null ? value.getClass().getName() : "null")
                    );
                }

                LOGGER.log(System.Logger.Level.DEBUG,
                        "Trocando imagem para: {0} em {1}",
                        new Object[]{annotation.imageId(), target.getClass().getSimpleName()});

                // Carrega imagem com ou sem tamanho
                if (annotation.width() > 0 && annotation.height() > 0) {
                    imageManager.load(imageView, annotation.imageId(),
                            annotation.width(), annotation.height());
                } else {
                    imageManager.load(imageView, annotation.imageId());
                }

            } catch (Exception e) {
                // FAIL-FAST: Nunca engolir exceções em handlers de UI
                throw new RuntimeException(
                        String.format("Falha ao processar @SwapImage para '%s'", annotation.imageId()),
                        e
                );
            }
        });
    }

    @Override
    public boolean isBeforePhase() {
        return AnnotationHandler.super.isBeforePhase();
    }

    @Override
    public boolean isAfterPhase() {
        return AnnotationHandler.super.isAfterPhase();
    }

    @Override
    public boolean isSuccessOnly() {
        return AnnotationHandler.super.isSuccessOnly();
    }

    @Override
    public boolean isErrorOnly() {
        return AnnotationHandler.super.isErrorOnly();
    }

    /**
     * Busca campo na hierarquia de classes (sobe até Object).
     */
    private Field findField(Class<?> clazz, String name) {
        var current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    // Os métodos de fase são herdados da interface com defaults:
    // - isBeforePhase() = false
    // - isAfterPhase() = true
    // - isSuccessOnly() = true
    // - isErrorOnly() = false
}