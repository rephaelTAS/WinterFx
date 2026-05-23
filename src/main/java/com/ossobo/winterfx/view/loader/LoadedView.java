package com.ossobo.winterfx.view.loader;

import com.ossobo.winterfx.view.refresh.RefreshableController;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 🎯 LOADED VIEW - Única classe para resultado do carregamento
 * ✅ Compatível com versão antiga
 * ✅ Suporte a múltiplos modos de uso
 */
public final class LoadedView<T> {
    private final Parent root;
    private final T controller;
    private final String sourcePath;
    private final boolean isDialogInstance;

    // Cache para evitar instanceof repetidos
    private final Boolean isRefreshable;
    private RefreshableController refreshableController;

    // ✅ CONSTRUTORES
    public LoadedView(Parent root, T controller, String sourcePath) {
        this(root, controller, sourcePath, false);
    }

    public LoadedView(Parent root, T controller, String sourcePath, boolean isDialogInstance) {
        this.root = Objects.requireNonNull(root, "Root não pode ser nulo");
        this.controller = controller;
        this.sourcePath = Objects.requireNonNull(sourcePath, "Source path não pode ser nulo");
        this.isDialogInstance = isDialogInstance;

        // Cache das verificações
        this.isRefreshable = controller instanceof RefreshableController;
        if (this.isRefreshable) {
            this.refreshableController = (RefreshableController) controller;
        }
    }

    // ✅ GETTERS SIMPLES
    public Parent getRoot() { return root; }
    public T getController() { return controller; }
    public String getSourcePath() { return sourcePath; }
    public boolean hasController() { return controller != null; }
    public boolean isDialogInstance() { return isDialogInstance; }
    public boolean isRefreshable() { return isRefreshable; }

    // ✅ CONFIGURAÇÃO FLUENTE
    public LoadedView<T> configure(Consumer<T> configurator) {
        if (configurator != null && hasController()) {
            configurator.accept(controller);
        }
        return this;
    }

    // ✅ MÉTODOS DO CICLO DE VIDA
    public void refreshIfNeeded() {
        if (isRefreshable && refreshableController != null) {
            refreshableController.refreshData();
        }
    }

    public void notifyViewShown() {
        if (isRefreshable && refreshableController != null) {
            refreshableController.onViewShown();
        }
    }

    public void notifyViewHidden() {
        if (isRefreshable && refreshableController != null) {
            refreshableController.onViewHidden();
        }
    }

    public void notifyViewInitialized() {
        if (isRefreshable && refreshableController != null) {
            refreshableController.onViewInitialized();
        }
    }

    // ✅ CONVERSÃO SEGURA DE TIPO
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

    // ✅ VERIFICAÇÃO DE TIPO
    public boolean isControllerOfType(Class<?> type) {
        return hasController() && type.isInstance(controller);
    }

    // ✅ DETACH (importante para diálogos)
    public void detachFromScene() {
        if (root.getScene() != null && isDialogInstance) {
            try {
                // Criar um placeholder vazio
                Pane placeholder = new Pane();
                placeholder.setPrefSize(root.getBoundsInParent().getWidth(),
                        root.getBoundsInParent().getHeight());

                // Substituir na cena
                root.getScene().setRoot(placeholder);

                // Limpar referências
                root.getChildrenUnmodifiable().clear();

            } catch (Exception e) {
                // Ignorar erros no detach
            }
        }
    }

    // ✅ MÉTODO PARA OBTER CONTROLLER COMO RefreshableController
    public RefreshableController getAsRefreshable() {
        return refreshableController;
    }

    @Override
    public String toString() {
        return String.format("LoadedView{path=%s, controller=%s, dialog=%s, refreshable=%s}",
                sourcePath,
                controller != null ? controller.getClass().getSimpleName() : "null",
                isDialogInstance,
                isRefreshable);
    }
}
