package com.ossobo.winterfx.imagemanager.image;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * 🎯 IMAGE LOADER - Com fallback programático robusto
 */
public class ImageLoader {

    /**
     * ✅ CARREGA IMAGEM DE RECURSO
     */
    public Optional<Image> loadFromResource(String resourcePath) {
        Objects.requireNonNull(resourcePath, "Resource path não pode ser nulo");

        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return Optional.empty();
            }

            Image image = new Image(stream);
            return Optional.of(image);

        } catch (Exception e) {
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