package com.ossobo.winterfx.view.binding;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

/**
 * Propriedade reativa que garante Thread-Safety automático no JavaFX.
 * Qualquer atualização feita fora da JavaFX Application Thread será
 * automaticamente delegada para o Platform.runLater().
 */
public class ThreadSafeStringProperty extends SimpleStringProperty {

    public ThreadSafeStringProperty() {
        super();
    }

    public ThreadSafeStringProperty(String initialValue) {
        super(initialValue);
    }

    @Override
    public void set(String newValue) {
        if (Platform.isFxApplicationThread()) {
            super.set(newValue);
        } else {
            Platform.runLater(() -> super.set(newValue));
        }
    }

    @Override
    public void setValue(String v) {
        set(v);
    }
}