// ImageLoader.java v3.0 - 2026-08-22
// Carregador de imagens com Fail-Fast
package com.ossobo.winterfx.imagemanager.image;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * Carregador de imagens com Fail-Fast e fallback limitado.
 *
 * <p><b>Características:</b></p>
 * <ul>
 *   <li>✅ Fail-Fast: exceções são propagadas</li>
 *   <li>✅ Validação de recursos</li>
 *   <li>✅ Sem fallback silencioso (o chamador decide)</li>
 * </ul>
 *
 * @version 3.0 (22/08/2026) - Fail-Fast, removido código morto, sem fallback
 */
public final class ImageLoader {

    private final ClassLoader classLoader;

    public ImageLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public ImageLoader(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader não pode ser null");
    }

    /**
     * Carrega imagem de recurso com Fail-Fast.
     *
     * @param resourcePath Caminho do recurso (ex: "/images/logo.png")
     * @return Imagem carregada
     * @throws IllegalArgumentException Se recurso não for encontrado
     * @throws RuntimeException Se houver erro de I/O
     */
    public Optional<Image> loadFromResource(String resourcePath) {
        ImageUtils.validateResourcePath(resourcePath);

        var normalizedPath = ImageUtils.normalizePath(resourcePath);
        if (normalizedPath == null) {
            throw new IllegalArgumentException("Caminho de recurso inválido: " + resourcePath);
        }

        try (InputStream stream = classLoader.getResourceAsStream(normalizedPath)) {
            if (stream == null) {
                // Fail-Fast: não retorna Optional.empty() silenciosamente
                throw new IllegalArgumentException(
                        String.format("Recurso de imagem não encontrado no classpath: %s", normalizedPath)
                );
            }

            var image = new Image(stream);
            if (image.isError()) {
                throw new RuntimeException(
                        String.format("Erro ao decodificar imagem: %s (código: %d)",
                                normalizedPath, image.getException() != null ?
                                        image.getException().hashCode() : -1)
                );
            }

            return Optional.of(image);

        } catch (IllegalArgumentException e) {
            throw e; // Propaga erro de negócio
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Falha de I/O ao carregar imagem: %s", normalizedPath),
                    e
            );
        }
    }

    /**
     * Carrega imagem com fallback definido pelo chamador.
     */
    public Image loadWithFallback(String resourcePath, Image fallback) {
        try {
            return loadFromResource(resourcePath)
                    .orElse(fallback);
        } catch (Exception e) {
            return fallback;
        }
    }
}