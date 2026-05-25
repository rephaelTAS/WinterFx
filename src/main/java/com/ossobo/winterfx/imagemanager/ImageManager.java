package com.ossobo.winterfx.imagemanager;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.di.annotations.Component;
import com.ossobo.winterfx.di.annotations.InjectImage;
import com.ossobo.winterfx.di.annotations.ScopeAnnotation;
import com.ossobo.winterfx.di.scopes.enums.ScopeType;
import com.ossobo.winterfx.imagemanager.image.ImageCache;
import com.ossobo.winterfx.imagemanager.image.ImageLoader;
import com.ossobo.winterfx.imagemanager.image.ImageViewFactory;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@ScopeAnnotation(ScopeType.SINGLETON)
public class ImageManager {

    private static final Logger LOGGER = Logger.getLogger(ImageManager.class.getName());

    private final ResourceRegistry registry;
    private final DiContainer diContainer;
    private final ImageCache imageCache;
    private final ImageLoader imageLoader;
    private final ImageViewFactory viewFactory;

    public ImageManager(ResourceRegistry registry) {
        this.registry = registry;
        this.diContainer = DiContainer.getInstance();
        this.imageCache = new ImageCache();
        this.imageLoader = new ImageLoader();
        this.viewFactory = new ImageViewFactory();
        LOGGER.info("🖼️ ImageManager v3.1 inicializado");
    }

    // =============================================
    // PROCESSAMENTO DE @InjectImage
    // =============================================

    public void processAnnotations(Object bean) {
        if (bean == null) return;
        Class<?> clazz = bean.getClass();
        int count = 0;
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            InjectImage ann = field.getAnnotation(InjectImage.class);
            if (ann != null) { processInjectImage(bean, field, ann); count++; }
        }
        if (count > 0) LOGGER.log(Level.FINE, "🖼️ {0} @InjectImage em {1}", new Object[]{count, clazz.getSimpleName()});
    }

    private void processInjectImage(Object bean, java.lang.reflect.Field field, InjectImage annotation) {
        String imageId = annotation.value();
        try {
            Optional<ImageDescriptor> opt = registry.findImageById(imageId);
            if (opt.isEmpty()) {
                if (annotation.required()) throw new IllegalArgumentException("Imagem não registrada: '" + imageId + "'");
                LOGGER.warning("⚠️ Imagem não encontrada: '" + imageId + "'");
                return;
            }
            ImageDescriptor descriptor = opt.get();

            // 🔥 Se o campo JÁ TEM um ImageView (do FXML), apenas carrega a imagem nele!
            field.setAccessible(true);
            Object currentValue = field.get(bean);
            if (currentValue instanceof ImageView imageView) {
                Image image = loadImage(imageId, descriptor,
                        annotation.width() > 0 ? annotation.width() : descriptor.getPreferredWidth(),
                        annotation.height() > 0 ? annotation.height() : descriptor.getPreferredHeight(),
                        annotation.preserveRatio(), annotation.smooth(), annotation.cache());
                imageView.setImage(image);
                if (annotation.width() > 0) imageView.setFitWidth(annotation.width());
                if (annotation.height() > 0) imageView.setFitHeight(annotation.height());
                imageView.setPreserveRatio(annotation.preserveRatio());
                LOGGER.info("🖼️ Imagem no ImageView FXML: '" + imageId + "' → " + field.getName());
                return;
            }

            // Carrega e injeta (sync ou async)
            if (annotation.async()) {
                loadAndInjectAsync(bean, field, imageId, descriptor, annotation);
            } else {
                loadAndInject(bean, field, imageId, descriptor, annotation);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Erro @InjectImage '" + imageId + "': " + e.getMessage(), e);
            if (annotation.required()) throw new RuntimeException("Falha: " + imageId, e);
        }
    }

    // =============================================
    // INJEÇÃO SÍNCRONA
    // =============================================

    private void loadAndInject(Object bean, java.lang.reflect.Field field,
                               String imageId, ImageDescriptor descriptor,
                               InjectImage annotation) throws IllegalAccessException {
        double w = annotation.width() > 0 ? annotation.width() : descriptor.getPreferredWidth();
        double h = annotation.height() > 0 ? annotation.height() : descriptor.getPreferredHeight();
        Image image = loadImage(imageId, descriptor, w, h, annotation.preserveRatio(), annotation.smooth(), annotation.cache());
        field.setAccessible(true);
        Class<?> ft = field.getType();
        if (annotation.asBackground() || Background.class.isAssignableFrom(ft)) {
            field.set(bean, createBackground(image));
        } else if (ImageView.class.isAssignableFrom(ft)) {
            field.set(bean, viewFactory.create(image, w, h, annotation.preserveRatio(), annotation.smooth()));
        } else if (Image.class.isAssignableFrom(ft)) {
            field.set(bean, image);
        }
        LOGGER.info("✅ Imagem injetada: '" + imageId + "' → " + field.getName());
    }

    // =============================================
    // INJEÇÃO ASSÍNCRONA
    // =============================================

    private void loadAndInjectAsync(Object bean, java.lang.reflect.Field field,
                                    String imageId, ImageDescriptor descriptor, InjectImage annotation) {
        double w = annotation.width() > 0 ? annotation.width() : descriptor.getPreferredWidth();
        double h = annotation.height() > 0 ? annotation.height() : descriptor.getPreferredHeight();
        URL url = descriptor.getImageUrl();
        CompletableFuture.supplyAsync(() -> w > 0 && h > 0 ? new Image(url.toExternalForm(), w, h, annotation.preserveRatio(), annotation.smooth()) : new Image(url.toExternalForm()))
                .thenAccept(image -> Platform.runLater(() -> {
                    try {
                        field.setAccessible(true);
                        if (ImageView.class.isAssignableFrom(field.getType()))
                            field.set(bean, viewFactory.create(image, w, h, annotation.preserveRatio(), annotation.smooth()));
                        else if (Image.class.isAssignableFrom(field.getType())) field.set(bean, image);
                    } catch (Exception e) { LOGGER.log(Level.SEVERE, "❌ Erro async: " + e.getMessage(), e); }
                }));
    }

    // =============================================
    // CARREGAMENTO DE IMAGEM
    // =============================================

    public Image loadImage(String imageId, ImageDescriptor descriptor, double w, double h,
                           boolean preserveRatio, boolean smooth, boolean useCache) {
        String key = imageId + "_" + (int) w + "x" + (int) h;
        if (useCache) { Optional<Image> c = imageCache.get(key); if (c.isPresent()) return c.get(); }
        URL url = descriptor.getImageUrl();
        try {
            Image img = w > 0 && h > 0 ? new Image(url.toExternalForm(), w, h, preserveRatio, smooth)
                    : w > 0 ? new Image(url.toExternalForm(), w, 0, preserveRatio, smooth)
                      : h > 0 ? new Image(url.toExternalForm(), 0, h, preserveRatio, smooth)
                        : new Image(url.toExternalForm());
            if (useCache && img != null && !img.isError()) imageCache.put(key, img);
            return img;
        } catch (Exception e) { return imageLoader.loadPlaceholder().orElse(null); }
    }

    public Image loadImage(String imageId) {
        Optional<ImageDescriptor> opt = registry.findImageById(imageId);
        if (opt.isPresent()) {
            ImageDescriptor d = opt.get();
            return loadImage(imageId, d, d.getPreferredWidth(), d.getPreferredHeight(), d.isPreserveRatio(), d.isSmooth(), true);
        }
        return imageLoader.loadPlaceholder().orElse(null);
    }

    public void load(ImageView target, String imageId) {
        Image img = loadImage(imageId);
        if (img != null && target != null) target.setImage(img);
    }

    public void load(ImageView target, String imageId, double w, double h) {
        Image img = loadImage(imageId);
        if (img != null && target != null) { target.setImage(img); target.setFitWidth(w); target.setFitHeight(h); }
    }

    public java.util.List<ImageDescriptor> listAllImages() { return registry.findAllImages(); }
    public boolean isRegistered(String id) { return registry.findImageById(id).isPresent(); }

    public Background createBackground(Image image) {
        return new Background(new BackgroundImage(image, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT));
    }

    public void clearCache() { imageCache.clear(); }
    public int getCacheSize() { return imageCache.size(); }
    public Map<String, Object> getStats() { return imageCache.getStats(); }
}