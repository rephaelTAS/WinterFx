package com.ossobo.winterfx.view.lifecycle;

import com.ossobo.winterfx.view.loader.LoadedView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 🧹 ViewStateDestroyer - Gerenciador de Ciclo de Vida MVVM
 *
 * <p>Responsável por destruir o estado reativo (ViewModel oculto) de uma View
 * quando ela é fechada ou removida do cache, garantindo que não haja
 * Memory Leaks no JavaFX.</p>
 *
 * <p><b>Regra:</b> Só destrói se a View possuir estado reativo (hasReactiveState).
 * Views legadas (MVC puro) são ignoradas seguramente.</p>
 *
 * @version 1.0
 */
public class ViewStateDestroyer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ViewStateDestroyer.class);

    /**
     * Destrói o estado reativo de uma view carregada.
     *
     * @param loadedView A view que está sendo descarregada/fechada
     */
    public void destroy(LoadedView<?> loadedView) {
        if (loadedView == null) {
            return;
        }

        if (loadedView.hasReactiveState()) {
            LOGGER.debug("🧹 [MVVM] Destruindo estado reativo oculto da view: {}", loadedView.getSourcePath());

            // Aciona o dispose() de todos os bindings bidirecionais que criamos
            loadedView.destroy();

            LOGGER.debug("✅ [MVVM] Memória limpa com sucesso para: {}", loadedView.getSourcePath());
        } else {
            LOGGER.trace("⚪ [LEGADO] View não possui estado reativo (Modo MVC). Nada a destruir: {}", loadedView.getSourcePath());
        }
    }
}