package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.imagemanager.ImageManager;
import com.ossobo.winterfx.imagemanager.anotations.InjectImage;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.lang.reflect.Field;
import java.util.List;

/**
 * ImageInjector v1.1
 *
 * Injetor de imagens via @InjectImage.
 * Se o campo já tem um ImageView (criado pelo JavaFX), apenas atualiza a imagem.
 * Caso contrário, cria um novo ImageView.
 */
public class ImageInjector implements DependencyInjector {

    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final ImageManager imageManager;

    public ImageInjector(ReflectionCache reflectionCache,
                         ReflectionProcessor reflectionProcessor,
                         ImageManager imageManager) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.imageManager = imageManager;
    }

    @Override
    public void inject(Object instance, Class<?> type) {
        List<Field> imageFields = reflectionCache.getInjectImageFields(type);

        for (Field field : imageFields) {
            InjectImage annotation = field.getAnnotation(InjectImage.class);
            String imageId = annotation.value();

            try {
                // Carrega a imagem
                Image image = imageManager.loadImage(imageId);

                if (image == null) {
                    if (annotation.required()) {
                        throw new IllegalArgumentException("Imagem não registrada: '" + imageId + "'");
                    }
                    continue;
                }

                // Verifica se o campo já tem um ImageView (criado pelo JavaFX)
                field.setAccessible(true);
                Object currentValue = field.get(instance);

                if (currentValue instanceof ImageView existingView) {
                    // ✅ ImageView já existe (JavaFX) → só atualiza a imagem
                    existingView.setImage(image);

                    if (annotation.width() > 0) existingView.setFitWidth(annotation.width());
                    if (annotation.height() > 0) existingView.setFitHeight(annotation.height());
                    existingView.setPreserveRatio(annotation.preserveRatio());
                    existingView.setSmooth(annotation.smooth());
                } else {
                    // 🆕 Campo vazio → cria novo ImageView
                    ImageView imageView = createImageView(image, annotation);
                    reflectionProcessor.injectField(instance, field, imageView);
                }

            } catch (Exception e) {
                if (annotation.required()) {
                    throw new RuntimeException("Falha ao injetar imagem: " + imageId, e);
                }
            }
        }
    }

    /**
     * Cria um ImageView configurado com as dimensões da anotação.
     */
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