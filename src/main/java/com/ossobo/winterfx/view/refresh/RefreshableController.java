// RefreshableController.java
package com.ossobo.winterfx.view.refresh;

/**
 * ✅ Interface unificada para controllers que podem ser atualizados
 * Compatível com versão antiga FXMLManager.RefreshableController
 */
public interface RefreshableController {
    /**
     * Atualiza os dados do controller (método principal)
     */
    void refreshData();

    /**
     * Chamado quando a view é exibida
     */
    default void onViewShown() {}

    /**
     * Chamado quando a view é fechada
     */
    default void onViewHidden() {}

    /**
     * Chamado quando a view é inicializada
     */
    default void onViewInitialized() {}
}
