package com.ossobo.winterfx.view.design;

import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import javafx.scene.Parent;

import java.net.URL;
import java.util.List;

/**
 * 🎨 StyleManager v2.1
 *
 * Aplica CSS do ViewDescriptor ao Parent.
 * NÃO limpa stylesheets existentes — apenas adiciona.
 */
public final class StyleManager {

    private static final StyleManager INSTANCE = new StyleManager();

    private StyleManager() {
    }

    public static StyleManager getInstance() {
        return INSTANCE;
    }

    /**
     * Aplica CSS do ViewDescriptor ao Parent.
     * Apenas ADICIONA — não remove CSS existentes.
     */
    public void apply(Parent root, ViewDescriptor descriptor) {
        if (root == null || descriptor == null) {
            return;
        }

        int appliedCount = 0;

        // CSS primário
        URL primaryCss = descriptor.getPrimaryCss();
        if (primaryCss != null && !containsStylesheet(root, primaryCss)) {
            root.getStylesheets().add(primaryCss.toExternalForm());
            appliedCount++;
        }

        // CSS adicionais
        List<URL> additionalCss = descriptor.getAdditionalCss();
        if (additionalCss != null) {
            for (URL additional : additionalCss) {
                if (additional != null && !containsStylesheet(root, additional)) {
                    root.getStylesheets().add(additional.toExternalForm());
                    appliedCount++;
                }
            }
        }
    }

    /**
     * Remove todos os stylesheets do root.
     */
    public void clear(Parent root) {
        if (root == null) return;
        int count = root.getStylesheets().size();
        root.getStylesheets().clear();
    }

    private boolean containsStylesheet(Parent root, URL cssUrl) {
        return root.getStylesheets().contains(cssUrl.toExternalForm());
    }
}