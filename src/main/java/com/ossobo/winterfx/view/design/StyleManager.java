// StyleManager.java v2.3 - 2026-10-09
// Ordem corrigida: additionalCss (fundação) → primaryCss (específico da view)
package com.ossobo.winterfx.view.design;

import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import javafx.scene.Parent;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.util.List;

/**
 * 🎨 StyleManager v2.3
 *
 * <p>Aplica CSS do {@link ViewDescriptor} ao {@link Parent}.</p>
 *
 * <p><b>Ordem de aplicação (importa):</b></p>
 * <ol>
 *   <li>{@code additionalCss[0..n]} — fundação: tokens, componentes, layout</li>
 *   <li>{@code primaryCss} — específico da view, aplicado por ÚLTIMO</li>
 * </ol>
 *
 * <p>No JavaFX, a ordem de registo em {@code getStylesheets()} decide quem
 * sobrepõe quem (o último ganha em empates). Por isso o CSS específico da view
 * é aplicado depois da fundação — para poder refinar sem ser apagado.</p>
 *
 * @version 2.3 (09/10/2026) — Ordem de aplicação corrigida
 */
public final class StyleManager {

    private static final Logger LOGGER = System.getLogger(StyleManager.class.getName());

    private static final StyleManager INSTANCE = new StyleManager();

    public StyleManager() {
    }

    public static StyleManager getInstance() {
        return INSTANCE;
    }

    // ============================================================
    // APLICAÇÃO
    // ============================================================

    /**
     * Aplica o CSS do {@link ViewDescriptor} ao {@link Parent}.
     *
     * <p>Limpa os stylesheets existentes antes de aplicar — o resultado é
     * sempre determinístico: mesmo descriptor → mesmos stylesheets.</p>
     *
     * <p><b>Ordem:</b> {@code additionalCss} primeiro (fundação),
     * {@code primaryCss} por último (específico da view).</p>
     *
     * @param root       raiz a estilizar (ignorado se {@code null})
     * @param descriptor metadados da view (ignorado se {@code null})
     */
    public void apply(Parent root, ViewDescriptor descriptor) {
        if (root == null || descriptor == null) {
            return;
        }

        // Limpar stylesheets existentes
        root.getStylesheets().clear();

        int appliedCount = 0;

        // 1. CSS adicionais (fundação) — na ordem declarada
        List<URL> additionalCss = descriptor.additionalCss();
        if (additionalCss != null && !additionalCss.isEmpty()) {
            for (URL additional : additionalCss) {
                if (additional != null) {
                    root.getStylesheets().add(additional.toExternalForm());
                    appliedCount++;
                }
            }
        }

        // 2. CSS primário (específico da view) — aplicado por ÚLTIMO
        URL primaryCss = descriptor.primaryCss();
        if (primaryCss != null) {
            root.getStylesheets().add(primaryCss.toExternalForm());
            appliedCount++;
        }

        LOGGER.log(Level.DEBUG, "CSS aplicado em '{0}': {1} ficheiro(s) [{2} additional + {3} primary]",
                descriptor.id(),
                appliedCount,
                additionalCss != null ? additionalCss.size() : 0,
                primaryCss != null ? 1 : 0);
    }

    /**
     * Aplica CSS diretamente por paths, sem passar por {@link ViewDescriptor}.
     *
     * <p>Mantém a ordem em que os paths são passados — o último aplicado
     * sobrepõe os anteriores. Ignora paths {@code null} ou não encontrados.</p>
     *
     * @param root     raiz a estilizar (ignorado se {@code null})
     * @param cssPaths caminhos no classpath (ex: {@code "/css/login.css"})
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
            } else {
                LOGGER.log(Level.WARNING, "CSS não encontrado no classpath: {0}", cssPath);
            }
        }
    }

    /**
     * Remove todos os stylesheets do {@link Parent}.
     */
    public void clear(Parent root) {
        if (root == null) return;
        root.getStylesheets().clear();
    }

    // ============================================================
    // INTERNO
    // ============================================================

    /**
     * Resolve um path de classpath em URL.
     * Paths relativos são normalizados com {@code /} inicial.
     */
    private URL resolveUrl(String cssPath) {
        if (cssPath == null || cssPath.isBlank()) {
            return null;
        }

        String normalizedPath = cssPath.startsWith("/") ? cssPath : "/" + cssPath;
        return getClass().getResource(normalizedPath);
    }
}