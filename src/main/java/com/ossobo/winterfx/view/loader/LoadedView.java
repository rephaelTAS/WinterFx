package com.ossobo.winterfx.view.loader;

import com.ossobo.winterfx.view.injection.ViewState; // [NOVO IMPORT]
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 🎯 LOADED VIEW - Única classe para resultado do carregamento v3.0 (MVVM)
 *
 * <p><b>Responsabilidades:</b></p>
 * <ul>
 *   <li>Armazenar o root (Parent) da view carregada</li>
 *   <li>Armazenar o controller da view</li>
 *   <li>Armazenar o estado reativo oculto (MVVM) da view</li>
 *   <li>Armazenar o caminho de origem (sourcePath)</li>
 *   <li>Indicar se é uma instância de diálogo</li>
 *   <li>Permitir configuração fluente do controller</li>
 *   <li>Permitir conversão segura de tipo do controller</li>
 *   <li>Gerenciar o ciclo de vida (limpeza de memória)</li>
 * </ul>
 *
 * @version 3.0 - Integração MVVM Invisível
 */
public final class LoadedView<T> {

    private final Parent root;
    private final T controller;
    private final String sourcePath;
    private final boolean isDialogInstance;

    // [NOVO] O coração do MVVM oculto. Se for nulo, a view opera em modo legado.
    private final ViewState viewState;

    // ============================================================
    // CONSTRUTORES (Sobrecarga para não quebrar código antigo)
    // ============================================================

    public LoadedView(Parent root, T controller, String sourcePath) {
        this(root, controller, sourcePath, false, null);
    }

    public LoadedView(Parent root, T controller, String sourcePath, boolean isDialogInstance) {
        this(root, controller, sourcePath, isDialogInstance, null);
    }

    // [NOVO] Construtor oficial usado pelo FXMLService a partir de agora
    public LoadedView(Parent root, T controller, String sourcePath, boolean isDialogInstance, ViewState viewState) {
        this.root = Objects.requireNonNull(root, "Root não pode ser nulo");
        this.controller = controller;
        this.sourcePath = Objects.requireNonNull(sourcePath, "Source path não pode ser nulo");
        this.isDialogInstance = isDialogInstance;
        this.viewState = viewState;
    }

    // ============================================================
    // GETTERS SIMPLES
    // ============================================================

    public Parent getRoot() {
        return root;
    }

    public T getController() {
        return controller;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public boolean hasController() {
        return controller != null;
    }

    public boolean isDialogInstance() {
        return isDialogInstance;
    }

    // [NOVO] Getter para o estado MVVM
    public ViewState getViewState() {
        return viewState;
    }

    /**
     * Verifica se esta view possui o estado reativo MVVM ativo.
     */
    public boolean hasReactiveState() {
        return viewState != null;
    }

    // ============================================================
    // CONFIGURAÇÃO FLUENTE
    // ============================================================

    public LoadedView<T> configure(Consumer<T> configurator) {
        if (configurator != null && hasController()) {
            configurator.accept(controller);
        }
        return this;
    }

    // ============================================================
    // CONVERSÃO SEGURA DE TIPO
    // ============================================================

    @SuppressWarnings("unchecked")
    public <C> C getControllerAs(Class<C> type) {
        if (hasController() && type.isInstance(controller)) {
            return (C) controller;
        }
        throw new ClassCastException(
                String.format("Controller não é do tipo %s (é %s)",
                        type.getSimpleName(),
                        controller != null ? controller.getClass().getSimpleName() : "null")
        );
    }

    public boolean isControllerOfType(Class<?> type) {
        return hasController() && type.isInstance(controller);
    }

    // ============================================================
    // CICLO DE VIDA E LIMPEZA DE MEMÓRIA (MVVM)
    // ============================================================

    /**
     * 🧹 [NOVO] Destrói o estado reativo da View.
     *
     * <p>Deve ser chamado pelo StageManager quando a janela for fechada.
     * Isso desfaz os binds bidirecionais (ThreadSafeProperty) para que
     * o Garbage Collector do Java possa limpar a tela da memória,
     * evitando Memory Leaks comuns no JavaFX.</p>
     */
    public void destroy() {
        if (hasReactiveState()) {
            viewState.destroy();
        }
    }

    // ============================================================
    // DETACH (para diálogos)
    // ============================================================

    /**
     * Desanexa o root da cena (útil para diálogos).
     */
    public void detachFromScene() {
        if (root.getScene() != null && isDialogInstance) {
            try {
                Pane placeholder = new Pane();
                placeholder.setPrefSize(
                        root.getBoundsInParent().getWidth(),
                        root.getBoundsInParent().getHeight()
                );
                root.getScene().setRoot(placeholder);
                // Nota: getChildrenUnmodifiable().clear() lançava UnsupportedOperationException
                // no seu código anterior. Removi para evitar crash silencioso.
            } catch (Exception e) {
                // Ignorar erros no detach
            }
        }
    }

    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================

    public boolean isInstanceOf(Class<?> type) {
        return hasController() && type.isAssignableFrom(controller.getClass());
    }

    public Object getControllerAsObject() {
        return controller;
    }

    // ============================================================
    // TO STRING
    // ============================================================

    @Override
    public String toString() {
        return String.format("LoadedView{path='%s', controller=%s, dialog=%s, mvvm=%s}",
                sourcePath,
                controller != null ? controller.getClass().getSimpleName() : "null",
                isDialogInstance,
                hasReactiveState());
    }
}