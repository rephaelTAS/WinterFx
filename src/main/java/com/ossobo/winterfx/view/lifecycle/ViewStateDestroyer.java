// ViewStateDestroyer.java v1.1 - 2026-08-22
// Corrigido para API nativa java.lang.System.Logger
package com.ossobo.winterfx.view.lifecycle;

import com.ossobo.winterfx.view.loader.LoadedView;

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
 * @version 1.1 (22/08/2026) - Ajuste para System.Logger nativo
 */
public class ViewStateDestroyer {

    // CORREÇÃO 1: Exige String, não Class
    private static final System.Logger LOGGER = System.getLogger(ViewStateDestroyer.class.getName());

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
            // CORREÇÃO 2: Usa log(Level, ...) e placeholder {0}
            LOGGER.log(System.Logger.Level.DEBUG, "🧹 [MVVM] Destruindo estado reativo oculto da view: {0}", loadedView.sourcePath());

            // Aciona o dispose() de todos os bindings bidirecionais que criamos
            loadedView.destroy();

            LOGGER.log(System.Logger.Level.DEBUG, "✅ [MVVM] Memória limpa com sucesso para: {0}", loadedView.sourcePath());
        } else {
            // CORREÇÃO 3: Usa TRACE level
            LOGGER.log(System.Logger.Level.TRACE, "⚪ [LEGADO] View não possui estado reativo (Modo MVC). Nada a destruir: {0}", loadedView.sourcePath());
        }
    }
}