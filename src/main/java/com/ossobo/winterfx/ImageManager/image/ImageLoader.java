package com.ossobo.winterfx.ImageManager.image;

import com.ossobo.winterfx.di.annotations.Component;
import com.ossobo.winterfx.di.annotations.ScopeAnnotation;

import com.ossobo.winterfx.di.scopes.enums.ScopeType;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * 🎯 IMAGE LOADER - Com fallback programático robusto
 */
@Component
@ScopeAnnotation(ScopeType.SINGLETON)
public class ImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageLoader.class);

    /**
     * ✅ CARREGA IMAGEM DE RECURSO
     */
    public Optional<Image> loadFromResource(String resourcePath) {
        Objects.requireNonNull(resourcePath, "Resource path não pode ser nulo");

        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                LOGGER.warn("Recurso não encontrado: {}", resourcePath);
                return Optional.empty();
            }

            Image image = new Image(stream);
            LOGGER.debug("Imagem carregada: {}", resourcePath);
            return Optional.of(image);

        } catch (Exception e) {
            LOGGER.warn("Falha ao carregar recurso: {}", resourcePath, e);
            return Optional.empty();
        }
    }

    /**
     * ✅ CARREGA IMAGEM PLACEHOLDER
     */
    public Optional<Image> loadPlaceholder() {
        return loadFromResource("/images/placeholder.png");
    }


    /**
     * 🔥 FALLBACK PROGRAMÁTICO DE EMERGÊNCIA
     */
    private Image createMinimalFallback() {
        // Imagem 1x1 pixel transparente como último recurso
        String minimalImage = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
        return new Image(minimalImage);
    }
}