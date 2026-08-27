// ImageViewFactory.java v3.0 - 2026-08-22
// Fábrica de ImageView (mantido, já estava em conformidade)
package com.ossobo.winterfx.imagemanager.image;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;

/**
 * Fábrica para criação e configuração de ImageView.
 *
 * @version 3.0 (22/08/2026) - Mantido, já estava em conformidade
 */
public final class ImageViewFactory {

    public record ViewConfig(
            double width,
            double height,
            boolean preserveRatio,
            boolean smooth
    ) {
        public static ViewConfig defaults() {
            return new ViewConfig(0, 0, true, true);
        }
    }

    public ImageView create(Image image) {
        return create(image, ViewConfig.defaults());
    }

    public ImageView create(Image image, double width, double height) {
        return create(image, new ViewConfig(width, height, true, true));
    }

    public ImageView create(Image image, ViewConfig config) {
        Objects.requireNonNull(image, "Image não pode ser nula");
        Objects.requireNonNull(config, "Config não pode ser nula");

        var imageView = new ImageView(image);
        applyConfig(imageView, config);
        return imageView;
    }

    public void applyConfig(ImageView imageView, ViewConfig config) {
        Objects.requireNonNull(imageView, "ImageView não pode ser nulo");
        Objects.requireNonNull(config, "Config não pode ser nula");

        if (config.width() > 0) {
            imageView.setFitWidth(config.width());
        }
        if (config.height() > 0) {
            imageView.setFitHeight(config.height());
        }
        imageView.setPreserveRatio(config.preserveRatio());
        imageView.setSmooth(config.smooth());
    }

    public void updateImage(ImageView imageView, Image newImage) {
        Objects.requireNonNull(imageView, "ImageView não pode ser nulo");
        Objects.requireNonNull(newImage, "Image não pode ser nula");

        imageView.setImage(newImage);
    }
}