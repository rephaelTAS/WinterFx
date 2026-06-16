package com.ossobo.winterfx.imagemanager;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.anotations.Component;
import com.ossobo.winterfx.anotations.Scope;
import com.ossobo.winterfx.di.scopes.enums.ScopeType;
import com.ossobo.winterfx.imagemanager.image.ImageCache;
import com.ossobo.winterfx.imagemanager.image.ImageLoader;
import com.ossobo.winterfx.imagemanager.image.ImageViewFactory;
import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;

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

@Component
@Scope(ScopeType.SINGLETON)
public class ImageManager {

    private final ResourceRegistry registry;
    private final DiContainer diContainer;
    private final ImageCache imageCache;
    private final ImageLoader imageLoader;
    private final ImageViewFactory viewFactory;

    public ImageManager(ResourceRegistry registry, DiContainer diContainer) {
        this.registry = registry;
        this.diContainer = diContainer;
        this.imageCache = new ImageCache();
        this.imageLoader = new ImageLoader();
        this.viewFactory = new ImageViewFactory();
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