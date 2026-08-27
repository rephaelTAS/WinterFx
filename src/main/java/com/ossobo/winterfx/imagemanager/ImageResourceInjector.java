// ImageResourceInjector.java v4.0 - 2026-08-22
// Injetor de imagens com System.Logger (Java 9+)
package com.ossobo.winterfx.imagemanager;

import com.ossobo.winterfx.di.injection.DependencyInjector;
import com.ossobo.winterfx.imagemanager.anotations.InjectImage;
import com.ossobo.winterfx.scanner.ReflectionScanner;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * ImageResourceInjector v4.0 — DESACOPLADO + JPMS-SAFE + System.Logger
 *
 * <p>Injetor de imagens via {@code @InjectImage}.</p>
 *
 * <p><b>Características:</b></p>
 * <ul>
 *   <li>✅ JPMS-Safe: canAccess() antes de setAccessible()</li>
 *   <li>✅ System.Logger (Java 9+) - Padrão WinterFX</li>
 *   <li>✅ Fail-Fast: obrigatórias lançam exceção</li>
 * </ul>
 *
 * @version 4.0 (22/08/2026) - System.Logger + JPMS-Safe
 */
public class ImageResourceInjector implements DependencyInjector {

    private static final System.Logger LOGGER = System.getLogger(ImageResourceInjector.class.getName());

    private final ReflectionScanner reflectionScanner;
    private final ImageManager imageManager;

    public ImageResourceInjector(ReflectionScanner reflectionScanner,
                                 ImageManager imageManager) {
        this.reflectionScanner = Objects.requireNonNull(reflectionScanner,
                "reflectionScanner não pode ser null");
        this.imageManager = Objects.requireNonNull(imageManager,
                "imageManager não pode ser null");
    }

    @Override
    public void inject(Object instance, Class<?> type) {
        Objects.requireNonNull(instance, "instance não pode ser null");
        Objects.requireNonNull(type, "type não pode ser null");

        var imageFields = reflectionScanner.getFieldsWithAnnotation(type, InjectImage.class);

        for (var field : imageFields) {
            var annotation = field.getAnnotation(InjectImage.class);
            var imageId = annotation.value();

            try {
                var image = imageManager.loadImage(imageId);

                if (image == null) {
                    if (annotation.required()) {
                        throw new IllegalArgumentException(
                                String.format("Imagem não registrada: '%s'", imageId)
                        );
                    }
                    LOGGER.log(System.Logger.Level.DEBUG, "Imagem opcional não encontrada: {0}", imageId);
                    continue;
                }

                // JPMS-SAFE: Verifica antes de setAccessible
                if (!field.canAccess(instance)) {
                    field.setAccessible(true);
                }

                var currentValue = field.get(instance);

                if (currentValue instanceof ImageView existingView) {
                    // ImageView já existe → atualiza
                    existingView.setImage(image);
                    applyViewConfig(existingView, annotation);

                } else {
                    // Campo vazio → cria novo ImageView
                    var imageView = createImageView(image, annotation);
                    field.set(instance, imageView);
                }

                LOGGER.log(System.Logger.Level.DEBUG, "Imagem injetada com sucesso: {0}", imageId);

            } catch (Exception e) {
                if (annotation.required()) {
                    throw new RuntimeException(
                            String.format("Falha crítica ao injetar imagem obrigatória: %s", imageId),
                            e
                    );
                }
                // System.Logger com nível WARNING para opcionais
                LOGGER.log(System.Logger.Level.WARNING,
                        "Falha ao injetar imagem opcional: " + imageId, e);
            }
        }
    }

    private ImageView createImageView(Image image, InjectImage annotation) {
        var imageView = new ImageView(image);
        applyViewConfig(imageView, annotation);
        return imageView;
    }

    private void applyViewConfig(ImageView imageView, InjectImage annotation) {
        var width = annotation.width();
        var height = annotation.height();

        if (width > 0) imageView.setFitWidth(width);
        if (height > 0) imageView.setFitHeight(height);

        imageView.setPreserveRatio(annotation.preserveRatio());
        imageView.setSmooth(annotation.smooth());
    }
}