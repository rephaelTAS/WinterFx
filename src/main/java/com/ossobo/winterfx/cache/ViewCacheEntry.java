package com.ossobo.winterfx.cache;

import com.ossobo.winterfx.view.injection.ViewState;
import com.ossobo.winterfx.view.loader.LoadedView;
import javafx.scene.Parent;

import java.util.Map;

/**
 * ViewCacheEntry v2.0 - Cache completo com root (CSS já aplicado) e namespace.
 *
 * <p><b>O que é armazenado:</b></p>
 * <ul>
 *   <li>root: Parent com CSS já aplicado</li>
 *   <li>namespace: Mapeamento fx:id → Node (para rebind O(1))</li>
 *   <li>controller: Controller da view (já com @FXML injetados)</li>
 *   <li>viewState: Estado MVVM (para destruição)</li>
 * </ul>
 *
 * @param viewId ID da view
 * @param root Root da view (com CSS aplicado)
 * @param namespace Mapeamento fx:id → Node (do FXMLLoader)
 * @param controller Controller da view
 * @param viewState Estado MVVM (para destruição)
 * @param timestamp Momento do carregamento
 * @param version Versão do cache
 */
public record ViewCacheEntry(
        String viewId,
        Parent root,
        Map<String, Object> namespace,
        Object controller,
        ViewState viewState,
        long timestamp,
        int version
) {
    public static ViewCacheEntry of(String viewId, Parent root, Map<String, Object> namespace,
                                    Object controller, ViewState viewState) {
        return new ViewCacheEntry(
                viewId,
                root,
                namespace, // Imutável!
                controller,
                viewState,
                System.currentTimeMillis(),
                1
        );
    }

    public ViewCacheEntry withVersion(int newVersion) {
        return new ViewCacheEntry(viewId, root, namespace, controller, viewState, timestamp, newVersion);
    }

    public boolean isValid() {
        return root != null && namespace != null && !namespace.isEmpty();
    }

    public LoadedView<?> toLoadedView() {
        return new LoadedView<>(root, controller, viewId, false, viewState);
    }

    @Override
    public String toString() {
        return String.format("ViewCacheEntry{viewId='%s', version=%d, namespaceSize=%d, timestamp=%d}",
                viewId, version, namespace != null ? namespace.size() : 0, timestamp);
    }
}