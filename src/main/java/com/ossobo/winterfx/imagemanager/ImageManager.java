// ImageManager.java v6.0 - 2026-08-23
// Fachada com ResourceModule (desacoplada de ResourceRegistry)
package com.ossobo.winterfx.imagemanager;

import com.ossobo.winterfx.imagemanager.image.ImageCache;
import com.ossobo.winterfx.imagemanager.image.ImageLoader;
import com.ossobo.winterfx.imagemanager.image.ImageUtils;
import com.ossobo.winterfx.imagemanager.image.ImageViewFactory;
import com.ossobo.winterfx.resources.ResourceModule;
import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;
import com.ossobo.winterfx.resources.enums.ResourceType;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * ImageManager v6.0 — Fachada do módulo imagemanager.
 *
 * <p><b>Características:</b></p>
 * <ul>
 *   <li>✅ System.Logger (Java 9+) - Padrão WinterFX</li>
 *   <li>✅ Transparência de falhas: erros de I/O são logados</li>
 *   <li>✅ Fail-Fast: exceções não são engolidas silenciosamente</li>
 *   <li>✅ Desacoplado: usa ResourceModule (Fachada) em vez de ResourceRegistry</li>
 * </ul>
 *
 * @version 6.0 (23/08/2026) - ResourceModule + API pública da Fachada
 */
public final class ImageManager {

    private static final System.Logger LOGGER = System.getLogger(ImageManager.class.getName());

    private final ResourceModule resourceModule;
    private final ImageCache imageCache;
    private final ImageLoader imageLoader;
    private final ImageViewFactory viewFactory;

    public record LoadOptions(
            double width,
            double height,
            boolean preserveRatio,
            boolean smooth,
            boolean useCache
    ) {
        public static LoadOptions defaults() {
            return new LoadOptions(0, 0, true, true, true);
        }
    }

    public ImageManager(ResourceModule resourceModule) {
        this.resourceModule = Objects.requireNonNull(resourceModule, "resourceModule não pode ser null");
        this.imageCache = new ImageCache();
        this.imageLoader = new ImageLoader();
        this.viewFactory = new ImageViewFactory();
    }

    // =============================================
    // CARREGAMENTO DE IMAGEM
    // =============================================

    /**
     * Carrega imagem com opções customizadas.
     *
     * <p><b>Transparência de falhas:</b> Se houver erro de I/O ou URL malformada,
     * o erro é logado em nível ERROR e retorna Optional.empty().</p>
     */
    public Optional<Image> loadImage(String imageId, LoadOptions options) {
        ImageUtils.validateKey(imageId);
        Objects.requireNonNull(options, "options não pode ser null");

        var cacheKey = buildCacheKey(imageId, options);

        // Verifica cache (Retorna Optional<Image>)
        if (options.useCache()) {
            var cached = imageCache.get(cacheKey);
            if (cached.isPresent()) {
                LOGGER.log(System.Logger.Level.DEBUG, "Cache hit: {0}", imageId);
                return cached;
            }
            LOGGER.log(System.Logger.Level.DEBUG, "Cache miss: {0}", imageId);
        }

        // ✅ Usa ResourceModule.requireImage() - Fail-Fast e O(1)
        try {
            var descriptor = resourceModule.requireImage(imageId);
            var image = loadFromDescriptor(descriptor, options);

            if (image != null && !image.isError()) {
                if (options.useCache()) {
                    imageCache.put(cacheKey, image);
                }
                return Optional.of(image);
            }

            LOGGER.log(System.Logger.Level.WARNING, "Imagem carregada com erro: {0}", imageId);
            return Optional.empty();

        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.ERROR,
                    "Falha ao carregar recurso de imagem: " + imageId, e);
            return Optional.empty();
        }
    }

    /**
     * Carrega imagem com dimensões especificadas.
     */
    public Image loadImage(String imageId, double width, double height) {
        return loadImage(imageId, new LoadOptions(width, height, true, true, true))
                .orElse(null);
    }

    /**
     * Carrega imagem com configurações padrão.
     */
    public Image loadImage(String imageId) {
        return loadImage(imageId, LoadOptions.defaults())
                .orElse(null);
    }

    /**
     * Carrega imagem em um ImageView existente.
     */
    public void load(ImageView target, String imageId) {
        Objects.requireNonNull(target, "target não pode ser null");

        var image = loadImage(imageId);
        if (image != null) {
            target.setImage(image);
        }
    }

    /**
     * Carrega imagem em um ImageView com dimensões.
     */
    public void load(ImageView target, String imageId, double width, double height) {
        Objects.requireNonNull(target, "target não pode ser null");

        var image = loadImage(imageId, width, height);
        if (image != null) {
            target.setImage(image);
            if (width > 0) target.setFitWidth(width);
            if (height > 0) target.setFitHeight(height);
        }
    }

    // =============================================
    // MÉTODOS PRIVADOS
    // =============================================

    /**
     * Carrega imagem a partir do descritor.
     *
     * <p><b>Fail-Fast:</b> Exceções do construtor Image são propagadas.
     * Isso inclui URL inválida, I/O errors, etc.</p>
     */
    private Image loadFromDescriptor(ImageDescriptor descriptor, LoadOptions options) {
        var url = descriptor.url();
        if (url == null) {
            throw new IllegalStateException(
                    String.format("URL da imagem é nula no descritor para ID: %s", descriptor.id())
            );
        }

        var w = options.width();
        var h = options.height();
        var urlStr = url.toExternalForm();
        var ratio = options.preserveRatio();
        var smooth = options.smooth();

        if (w > 0 && h > 0) {
            return new Image(urlStr, w, h, ratio, smooth);
        }
        if (w > 0) {
            return new Image(urlStr, w, 0, ratio, smooth);
        }
        if (h > 0) {
            return new Image(urlStr, 0, h, ratio, smooth);
        }
        return new Image(urlStr);
    }

    /**
     * Gera chave de cache baseada nas opções.
     */
    private String buildCacheKey(String imageId, LoadOptions options) {
        return String.format("%s_%.0fx%.0f_%b_%b",
                imageId,
                options.width(),
                options.height(),
                options.preserveRatio(),
                options.smooth()
        );
    }

    // =============================================
    // CONSULTA
    // =============================================

    /**
     * Lista todas as imagens registradas.
     * Usa a API pública do ResourceModule.
     */
    public List<ImageDescriptor> listAllImages() {
        return resourceModule.getAllImages();
    }

    /**
     * Verifica se imagem está registrada.
     * Usa a API pública do ResourceModule.
     */
    public boolean isRegistered(String id) {
        return resourceModule.exists(id, ResourceType.IMAGE);
    }

    /**
     * Busca descritor de imagem por ID.
     * Usa a API pública do ResourceModule.
     */
    public Optional<ImageDescriptor> getDescriptor(String id) {
        return resourceModule.findImage(id);
    }

    // =============================================
    // BACKGROUND
    // =============================================

    public Background createBackground(Image image) {
        Objects.requireNonNull(image, "image não pode ser null");

        return new Background(new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                BackgroundSize.DEFAULT
        ));
    }

    // =============================================
    // CACHE
    // =============================================

    public void clearCache() {
        imageCache.clear();
        LOGGER.log(System.Logger.Level.DEBUG, "Cache limpo");
    }

    public int getCacheSize() {
        return imageCache.size();
    }

    public ImageCache.CacheStats getStats() {
        return imageCache.getStats();
    }

    /**
     * Retorna estatísticas do cache em formato legível.
     */
    public Map<String, Object> getCacheStatsMap() {
        var stats = imageCache.getStats();
        return Map.of(
                "currentSize", stats.currentSize(),
                "maxSize", stats.maxSize(),
                "hits", stats.hits(),
                "misses", stats.misses(),
                "evictions", stats.evictions(),
                "hitRatio", String.format("%.1f%%", stats.hitRatio() * 100)
        );
    }
}