package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.imagemanager.ImageManager;
import com.ossobo.winterfx.imagemanager.anotations.InjectImage;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ImageInjector v1.1
 *
 * Injetor de imagens via @InjectImage.
 * Se o campo já tem um ImageView (criado pelo JavaFX), apenas atualiza a imagem.
 * Caso contrário, cria um novo ImageView.
 */
public class ImageInjector implements DependencyInjector {

    private static final Logger LOGGER = Logger.getLogger(ImageInjector.class.getName());

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
                    LOGGER.warning("⚠️ Imagem não encontrada: '" + imageId + "'");
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

                    LOGGER.log(Level.FINE, "✅ Imagem atualizada no ImageView existente: '{0}' → {1}.{2}",
                            new Object[]{imageId, type.getSimpleName(), field.getName()});
                } else {
                    // 🆕 Campo vazio → cria novo ImageView
                    ImageView imageView = createImageView(image, annotation);
                    reflectionProcessor.injectField(instance, field, imageView);

                    LOGGER.log(Level.FINE, "✅ Imagem injetada: '{0}' → {1}.{2}",
                            new Object[]{imageId, type.getSimpleName(), field.getName()});
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "❌ Erro @InjectImage '" + imageId + "': " + e.getMessage(), e);
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