package com.ossobo.winterfx.imagemanager.image;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;

/**
 * 🎯 IMAGE VIEW FACTORY - Responsabilidade Única: Criar e configurar ImageViews
 */

public class ImageViewFactory {

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

        return imageView;
    }
}