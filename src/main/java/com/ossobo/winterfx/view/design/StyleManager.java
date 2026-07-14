// StyleManager.java v2.2 - 2026-06-18
package com.ossobo.winterfx.view.design;

import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import javafx.scene.Parent;

import java.net.URL;
import java.util.List;

/**
 * 🎨 StyleManager v2.2
 *
 * Aplica CSS do ViewDescriptor ao Parent.
 * Com logging e resolução de caminhos.
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
     * Limpa stylesheets existentes e aplica todos do descriptor.
     */
    public void apply(Parent root, ViewDescriptor descriptor) {
        if (root == null) {
            return;
        }

        if (descriptor == null) {
            return;
        }

        // Limpar stylesheets existentes
        root.getStylesheets().clear();

        int appliedCount = 0;

        // ✅ CSS primário
        URL primaryCss = descriptor.getPrimaryCss();
        if (primaryCss != null) {
            String cssUrl = primaryCss.toExternalForm();
            root.getStylesheets().add(cssUrl);
            appliedCount++;
        }

        // CSS adicionais
        List<URL> additionalCss = descriptor.getAdditionalCss();
        if (additionalCss != null && !additionalCss.isEmpty()) {
            for (URL additional : additionalCss) {
                if (additional != null) {
                    String cssUrl = additional.toExternalForm();
                    root.getStylesheets().add(cssUrl);
                    appliedCount++;
                }
            }
        }
    }

    /**
     * Aplica CSS diretamente por paths (sem ViewDescriptor).
     */
    public void applyDirect(Parent root, String... cssPaths) {
        if (root == null || cssPaths == null || cssPaths.length == 0) {
            return;
        }

        for (String cssPath : cssPaths) {
            URL cssUrl = resolveUrl(cssPath);
            if (cssUrl != null) {
                String url = cssUrl.toExternalForm();
                if (!root.getStylesheets().contains(url)) {
                    root.getStylesheets().add(url);
                }
            }
        }
    }

    /**
     * Remove todos os stylesheets do root.
     */
    public void clear(Parent root) {
        if (root == null) return;
        root.getStylesheets().clear();
    }

    /**
     * Resolve URL do CSS.
     */
    private URL resolveUrl(String cssPath) {
        if (cssPath == null || cssPath.isBlank()) {
            return null;
        }

        // Normalizar caminho
        String normalizedPath = cssPath.startsWith("/") ? cssPath : "/" + cssPath;
        URL url = getClass().getResource(normalizedPath);

        return url;
    }

    private boolean containsStylesheet(Parent root, URL cssUrl) {
        return root.getStylesheets().contains(cssUrl.toExternalForm());
    }
}