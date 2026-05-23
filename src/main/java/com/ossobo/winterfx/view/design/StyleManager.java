// StyleManager.java v2.0
// Responsabilidade: Aplicar CSS fornecido pelo usuário via ViewDescriptor
// Fonte única: ViewDescriptor (não mais ViewRegistry)
package com.ossobo.winterfx.view.design;

import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import javafx.scene.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;

/**
 * 🎯 STYLE MANAGER v2.0
 *
 * Aplica CSS diretamente do ViewDescriptor.
 * ViewRegistry removido — o descriptor já contém todas as URLs.
 *
 * Uso:
 *   StyleManager.getInstance().apply(root, descriptor);
 */
public final class StyleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleManager.class);
    private static final StyleManager INSTANCE = new StyleManager();

    private StyleManager() {
        LOGGER.info("🎨 StyleManager v2.0 — CSS direto do ViewDescriptor");
    }

    public static StyleManager getInstance() {
        return INSTANCE;
    }

    // ==================== API PÚBLICA ====================

    /**
     * Aplica CSS do ViewDescriptor ao Parent.
     *
     * Ordem de aplicação:
     * 1. Limpa stylesheets anteriores
     * 2. Aplica primaryCss (base)
     * 3. Aplica additionalCss em ordem (override — último vence)
     */
    public void apply(Parent root, ViewDescriptor descriptor) {
        if (root == null) {
            LOGGER.warn("Root nulo — CSS não aplicado");
            return;
        }

        if (descriptor == null) {
            LOGGER.warn("ViewDescriptor nulo — CSS não aplicado");
            return;
        }

        applyStyles(root, descriptor);
    }

    /**
     * Aplica CSS diretamente por paths (sem ViewDescriptor).
     * Útil para overlays, diálogos ou componentes dinâmicos.
     */
    public void applyDirect(Parent root, String... cssPaths) {
        if (root == null || cssPaths == null || cssPaths.length == 0) {
            return;
        }

        for (String cssPath : cssPaths) {
            URL cssUrl = resolveUrl(cssPath);
            if (cssUrl != null && !containsStylesheet(root, cssUrl)) {
                root.getStylesheets().add(cssUrl.toExternalForm());
            }
        }

        LOGGER.debug("CSS direto aplicado: {} arquivos", cssPaths.length);
    }

    /**
     * Remove todos os CSS gerenciados de um root.
     */
    public void clear(Parent root) {
        if (root == null) return;

        int count = root.getStylesheets().size();
        root.getStylesheets().clear();

        LOGGER.debug("CSS removido: {} arquivos", count);
    }

    // ==================== LÓGICA INTERNA ====================

    private void applyStyles(Parent root, ViewDescriptor descriptor) {
        int appliedCount = 0;

        root.getStylesheets().clear();

        URL primaryCss = descriptor.getPrimaryCss();
        if (primaryCss != null) {
            root.getStylesheets().add(primaryCss.toExternalForm());
            appliedCount++;
            LOGGER.debug("Primary CSS: {}", primaryCss.getPath());
        }

        List<URL> additionalCss = descriptor.getAdditionalCss();
        for (URL additional : additionalCss) {
            if (additional != null && !containsStylesheet(root, additional)) {
                root.getStylesheets().add(additional.toExternalForm());
                appliedCount++;
                LOGGER.debug("Additional CSS: {}", additional.getPath());
            }
        }

        LOGGER.info("CSS aplicado para '{}': {} arquivos", descriptor.getId(), appliedCount);
    }

    private URL resolveUrl(String cssPath) {
        if (cssPath == null || cssPath.isBlank()) {
            return null;
        }

        String normalizedPath = cssPath.startsWith("/") ? cssPath : "/" + cssPath;
        URL url = getClass().getResource(normalizedPath);

        if (url == null) {
            LOGGER.debug("CSS não encontrado: {}", normalizedPath);
        }

        return url;
    }

    private boolean containsStylesheet(Parent root, URL cssUrl) {
        String externalForm = cssUrl.toExternalForm();
        return root.getStylesheets().contains(externalForm);
    }
}