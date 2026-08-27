package com.ossobo.winterfx.view.loader;

import com.ossobo.winterfx.view.injection.ViewState;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 🎯 LoadedView v5.0 - Record imutável.
 *
 * Correção: detachFromScene sem código morto.
 */
public record LoadedView<T>(
        Parent root,
        T controller,
        String sourcePath,
        boolean isDialogInstance,
        ViewState viewState
) {
    public LoadedView {
        Objects.requireNonNull(root, "Root não pode ser nulo");
        Objects.requireNonNull(sourcePath, "Source path não pode ser nulo");
    }

    public LoadedView(Parent root, T controller, String sourcePath) {
        this(root, controller, sourcePath, false, null);
    }

    public LoadedView(Parent root, T controller, String sourcePath, boolean isDialogInstance) {
        this(root, controller, sourcePath, isDialogInstance, null);
    }

    public boolean hasController() {
        return controller != null;
    }

    public boolean hasReactiveState() {
        return viewState != null;
    }

    public LoadedView<T> configure(Consumer<T> configurator) {
        if (configurator != null && hasController()) {
            configurator.accept(controller);
        }
        return this;
    }

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

    public void destroy() {
        if (hasReactiveState()) {
            viewState.destroy();
        }
    }

    /**
     * ✅ CORREÇÃO: Removido código morto e falho. Basta desacoplar o root.
     */
    public void detachFromScene() {
        if (root.getScene() != null && isDialogInstance) {
            root.getScene().setRoot(new Pane());
        }
    }

    public boolean isInstanceOf(Class<?> type) {
        return hasController() && type.isAssignableFrom(controller.getClass());
    }

    public Object getControllerAsObject() {
        return controller;
    }

    @Override
    public String toString() {
        return String.format("LoadedView{path='%s', controller=%s, dialog=%s, mvvm=%s}",
                sourcePath,
                controller != null ? controller.getClass().getSimpleName() : "null",
                isDialogInstance,
                hasReactiveState());
    }
}