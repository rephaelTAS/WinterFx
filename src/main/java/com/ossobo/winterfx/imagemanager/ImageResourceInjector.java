package com.ossobo.winterfx.imagemanager;

import com.ossobo.winterfx.di.injection.DependencyInjector;
import com.ossobo.winterfx.imagemanager.anotations.InjectImage;
import com.ossobo.winterfx.scanner.ReflectionScanner;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.lang.reflect.Field;
import java.util.List;

/**
 * ImageResourceInjector v2.0 — DESACOPLADO
 *
 * <p>Injetor de imagens via {@code @InjectImage}.
 * Implementa {@link DependencyInjector} para ser registrado
 * como injector externo no {@code InjectionManager}.</p>
 *
 * <p>NÃO depende de {@code ReflectionCache} do DI.
 * Usa {@link ReflectionScanner} do módulo {@code scanner}
 * para descobrir campos anotados.</p>
 *
 * <p>Comportamento:</p>
 * <ul>
 *   <li>Se o campo já tem um {@code ImageView} (criado pelo JavaFX) → atualiza a imagem</li>
 *   <li>Se o campo está vazio → cria novo {@code ImageView} e injeta</li>
 * </ul>
 *
 * @version 2.0 (01/07/2026)
 */
public class ImageResourceInjector implements DependencyInjector {

    private final ReflectionScanner reflectionScanner;
    private final ImageManager imageManager;

    /**
     * @param reflectionScanner Scanner de reflexão do módulo scanner
     * @param imageManager      Gerenciador de imagens
     */
    public ImageResourceInjector(ReflectionScanner reflectionScanner,
                                 ImageManager imageManager) {
        this.reflectionScanner = reflectionScanner;
        this.imageManager = imageManager;
    }

    @Override
    public void inject(Object instance, Class<?> type) {
        List<Field> imageFields = reflectionScanner.getFieldsWithAnnotation(type, InjectImage.class);

        for (Field field : imageFields) {
            InjectImage annotation = field.getAnnotation(InjectImage.class);
            String imageId = annotation.value();

            try {
                Image image = imageManager.loadImage(imageId);

                if (image == null) {
                    if (annotation.required()) {
                        throw new IllegalArgumentException(
                                "Imagem não registrada: '" + imageId + "'");
                    }
                    continue;
                }

                field.setAccessible(true);
                Object currentValue = field.get(instance);

                if (currentValue instanceof ImageView existingView) {
                    // ImageView já existe (JavaFX) → só atualiza
                    existingView.setImage(image);

                    if (annotation.width() > 0)
                        existingView.setFitWidth(annotation.width());
                    if (annotation.height() > 0)
                        existingView.setFitHeight(annotation.height());
                    existingView.setPreserveRatio(annotation.preserveRatio());
                    existingView.setSmooth(annotation.smooth());

                } else {
                    // Campo vazio → cria novo ImageView
                    ImageView imageView = createImageView(image, annotation);
                    field.set(instance, imageView);
                }

            } catch (Exception e) {
                if (annotation.required()) {
                    throw new RuntimeException(
                            "Falha ao injetar imagem: " + imageId, e);
                }
            }
        }
    }

    private ImageView createImageView(Image image, InjectImage annotation) {
        ImageView imageView = new ImageView(image);

        double width = annotation.width();
        double height = annotation.height();

        if (width > 0) imageView.setFitWidth(width);
        if (height > 0) imageView.setFitHeight(height);

        imageView.setPreserveRatio(annotation.preserveRatio());
        imageView.setSmooth(annotation.smooth());

        return imageView;
    }
}