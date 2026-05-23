package com.ossobo.winterfx.ImageManager;

import com.ossobo.winterfx.di.annotations.InjectImage;
import com.ossobo.winterfx.ImageManager.image.ImageCache;
import com.ossobo.winterfx.ImageManager.image.ImageLoader;
import com.ossobo.winterfx.ImageManager.image.ImageViewFactory;
import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;
import com.ossobo.winterfx.resources.registry.ResourceRegistry;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;

import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 🖼️ ImageManager v2.0 - UNIFICADO
 *
 * Motor de injeção automática de imagens via @InjectImage.
 * Integra o melhor dos dois mundos:
 * - ImageCache (LRU + SoftReference)
 * - ImageLoader (fallback + placeholder)
 * - ImageViewFactory (criação de ImageViews)
 * - ResourceRegistry (catálogo unificado)
 * - @InjectImage (anotação de injeção)
 *
 * <p>Processa @InjectImage automaticamente nos beans.</p>
 */
public class ImageManager {

    // =============================================
    // DEPENDÊNCIAS
    // =============================================

    private final ResourceRegistry registry;
    private final ImageCache imageCache;
    private final ImageLoader imageLoader;
    private final ImageViewFactory viewFactory;

    // =============================================
    // CONSTRUTOR
    // =============================================

    public ImageManager(ResourceRegistry registry) {
        this.registry = registry;
        this.imageCache = new ImageCache();
        this.imageLoader = new ImageLoader();
        this.viewFactory = new ImageViewFactory();
    }

    // =============================================
    // PROCESSAMENTO DE ANOTAÇÕES (MÉTODO PRINCIPAL)
    // =============================================

    /**
     * Processa @InjectImage em um objeto.
     * Injetando imagens automaticamente nos campos anotados.
     */
    public void processAnnotations(Object bean) {
        if (bean == null) return;

        Class<?> clazz = bean.getClass();

        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            InjectImage annotation = field.getAnnotation(InjectImage.class);
            if (annotation != null) {
                processInjectImage(bean, field, annotation);
            }
        }
    }

    /**
     * Processa uma única anotação @InjectImage.
     */
    private void processInjectImage(Object bean, java.lang.reflect.Field field,
                                    InjectImage annotation) {
        String imageId = annotation.value();

        try {
            // Busca o ImageDescriptor no ResourceRegistry
            Optional<ImageDescriptor> optDescriptor = registry.findImageById(imageId);

            if (optDescriptor.isEmpty()) {
                if (annotation.required()) {
                    throw new IllegalArgumentException(
                            "Imagem não registrada: '" + imageId + "'"
                    );
                } else {
                    System.err.println("⚠️ Imagem não encontrada: '" + imageId + "'");
                    return;
                }
            }

            ImageDescriptor descriptor = optDescriptor.get();

            // Carrega e injeta (async ou sync)
            if (annotation.async()) {
                loadAndInjectAsync(bean, field, imageId, descriptor, annotation);
            } else {
                loadAndInject(bean, field, imageId, descriptor, annotation);
            }

        } catch (Exception e) {
            System.err.println("❌ Erro @InjectImage '" + imageId +
                    "' no campo '" + field.getName() + "': " + e.getMessage());
            if (annotation.required()) {
                throw new RuntimeException("Falha ao injetar imagem: " + imageId, e);
            }
        }
    }

    // =============================================
    // INJEÇÃO SÍNCRONA
    // =============================================

    private void loadAndInject(Object bean, java.lang.reflect.Field field,
                               String imageId, ImageDescriptor descriptor,
                               InjectImage annotation) throws IllegalAccessException {

        // Determina dimensões (anotação sobrescreve descriptor)
        double width = annotation.width() > 0 ? annotation.width() : descriptor.getPreferredWidth();
        double height = annotation.height() > 0 ? annotation.height() : descriptor.getPreferredHeight();
        boolean preserveRatio = annotation.preserveRatio();
        boolean smooth = annotation.smooth();

        // Carrega a imagem (com cache)
        Image image = loadImage(imageId, descriptor, width, height, preserveRatio, smooth,
                annotation.cache());

        // Injeta conforme o tipo do campo
        field.setAccessible(true);
        Class<?> fieldType = field.getType();

        if (annotation.asBackground() || Background.class.isAssignableFrom(fieldType)) {
            Background background = createBackground(image);
            field.set(bean, background);
            System.out.println("✅ Background injetado: '" + imageId + "' → " + field.getName());

        } else if (ImageView.class.isAssignableFrom(fieldType)) {
            ImageView imageView = viewFactory.create(image, width, height, preserveRatio, smooth);
            field.set(bean, imageView);
            System.out.println("✅ ImageView injetado: '" + imageId + "' → " + field.getName());

        } else if (Image.class.isAssignableFrom(fieldType)) {
            field.set(bean, image);
            System.out.println("✅ Image injetada: '" + imageId + "' → " + field.getName());

        } else {
            System.err.println("⚠️ Tipo não suportado para @InjectImage: " +
                    fieldType.getName());
        }
    }

    // =============================================
    // INJEÇÃO ASSÍNCRONA
    // =============================================

    private void loadAndInjectAsync(Object bean, java.lang.reflect.Field field,
                                    String imageId, ImageDescriptor descriptor,
                                    InjectImage annotation) {

        double width = annotation.width() > 0 ? annotation.width() : descriptor.getPreferredWidth();
        double height = annotation.height() > 0 ? annotation.height() : descriptor.getPreferredHeight();
        boolean preserveRatio = annotation.preserveRatio();
        boolean smooth = annotation.smooth();
        URL url = descriptor.getImageUrl();

        CompletableFuture.supplyAsync(() -> {
            if (width > 0 && height > 0) {
                return new Image(url.toExternalForm(), width, height, preserveRatio, smooth);
            } else {
                return new Image(url.toExternalForm());
            }
        }).thenAccept(image -> {
            Platform.runLater(() -> {
                try {
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();

                    if (ImageView.class.isAssignableFrom(fieldType)) {
                        ImageView imageView = viewFactory.create(image, width, height,
                                preserveRatio, smooth);
                        field.set(bean, imageView);
                    } else if (Image.class.isAssignableFrom(fieldType)) {
                        field.set(bean, image);
                    }
                    System.out.println("✅ Imagem injetada (async): '" + imageId + "'");
                } catch (Exception e) {
                    System.err.println("❌ Erro na injeção async: " + e.getMessage());
                }
            });
        });
    }

    // =============================================
    // CARREGAMENTO DE IMAGEM (COM CACHE)
    // =============================================

    /**
     * Carrega imagem com cache integrado.
     */
    public Image loadImage(String imageId, ImageDescriptor descriptor,
                           double width, double height,
                           boolean preserveRatio, boolean smooth, boolean useCache) {

        String cacheKey = buildCacheKey(imageId, width, height);

        // Verifica cache
        if (useCache) {
            Optional<Image> cached = imageCache.get(cacheKey);
            if (cached.isPresent()) return cached.get();
        }

        // Carrega a imagem
        URL url = descriptor.getImageUrl();
        Image image;

        try {
            if (width > 0 && height > 0) {
                image = new Image(url.toExternalForm(), width, height, preserveRatio, smooth);
            } else if (width > 0) {
                image = new Image(url.toExternalForm(), width, 0, preserveRatio, smooth);
            } else if (height > 0) {
                image = new Image(url.toExternalForm(), 0, height, preserveRatio, smooth);
            } else {
                image = new Image(url.toExternalForm());
            }

            // Armazena no cache
            if (useCache && image != null && !image.isError()) {
                imageCache.put(cacheKey, image);
            }

            return image;

        } catch (Exception e) {
            // Fallback para placeholder
            System.err.println("⚠️ Erro ao carregar imagem '" + imageId + "': " + e.getMessage());
            return imageLoader.loadPlaceholder().orElse(null);
        }
    }

    // =============================================
    // API PÚBLICA (COMPATÍVEL COM IMAGESERVICE)
    // =============================================

    /**
     * Carrega imagem por ID (sem dimensões específicas).
     */
    public Image loadImage(String imageId) {
        Optional<ImageDescriptor> optDescriptor = registry.findImageById(imageId);

        if (optDescriptor.isPresent()) {
            ImageDescriptor descriptor = optDescriptor.get();
            return loadImage(imageId, descriptor,
                    descriptor.getPreferredWidth(),
                    descriptor.getPreferredHeight(),
                    descriptor.isPreserveRatio(),
                    descriptor.isSmooth(),
                    true);
        }

        // Fallback
        return imageLoader.loadPlaceholder().orElse(null);
    }

    /**
     * Aplica imagem em ImageView.
     */
    public void load(ImageView target, String imageId) {
        Image image = loadImage(imageId);
        if (image != null && target != null) {
            target.setImage(image);
        }
    }

    /**
     * Aplica imagem em ImageView com tamanho.
     */
    public void load(ImageView target, String imageId, double width, double height) {
        Image image = loadImage(imageId);
        if (image != null && target != null) {
            target.setImage(image);
            target.setFitWidth(width);
            target.setFitHeight(height);
            target.setPreserveRatio(true);
        }
    }

    // =============================================
    // CRIAÇÃO DE COMPONENTES
    // =============================================

    public Background createBackground(Image image) {
        BackgroundImage bgImage = new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                BackgroundSize.DEFAULT
        );
        return new Background(bgImage);
    }

    public ImageView createImageView(Image image, double width, double height,
                                     boolean preserveRatio, boolean smooth) {
        return viewFactory.create(image, width, height, preserveRatio, smooth);
    }

    // =============================================
    // CACHE
    // =============================================

    private String buildCacheKey(String imageId, double width, double height) {
        return imageId + "_" + (int) width + "x" + (int) height;
    }

    public void clearCache() {
        imageCache.clear();
        System.out.println("🧹 Cache de imagens limpo");
    }

    public int getCacheSize() {
        return imageCache.size();
    }

    public Map<String, Object> getStats() {
        return imageCache.getStats();
    }
}