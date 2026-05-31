package com.ossobo.winterfx.imagemanager.image;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 🎯 IMAGE VIEW FACTORY - Responsabilidade Única: Criar e configurar ImageViews
 */

public class ImageViewFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageViewFactory.class);

    /**
     * ✅ CRIA IMAGEVIEW COM DIMENSÕES
     */
    public ImageView create(Image image, double width, double height) {
        Objects.requireNonNull(image, "Image não pode ser nula");

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        LOGGER.debug("ImageView criado: {}x{}", width, height);
        return imageView;
    }

    /**
     * ✅ CRIA IMAGEVIEW SIMPLES
     */
    public ImageView create(Image image) {
        Objects.requireNonNull(image, "Image não pode ser nula");

        ImageView imageView = new ImageView(image);
        imageView.setSmooth(true);
        imageView.setPreserveRatio(true);

        LOGGER.debug("ImageView simples criado");
        return imageView;
    }

    /**
     * ✅ CRIA IMAGEVIEW COM CONFIGURAÇÃO AVANÇADA
     */
    public ImageView create(Image image, double width, double height, boolean preserveRatio, boolean smooth) {
        Objects.requireNonNull(image, "Image não pode ser nula");

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(preserveRatio);
        imageView.setSmooth(smooth);

        LOGGER.debug("ImageView avançado criado");
        return imageView;
    }
}