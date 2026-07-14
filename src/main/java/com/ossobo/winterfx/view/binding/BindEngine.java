package com.ossobo.winterfx.view.binding;

import com.ossobo.winterfx.view.injection.ViewState;
import javafx.beans.binding.Bindings;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;

/**
 * Motor responsável por ler um componente JavaFX puro e transformá-lo em um
 * componente reativo ligado ao nosso ViewState (MVVM oculto).
 */
public class BindEngine {

    /**
     * Tenta fazer o bind automático baseado no tipo do componente do JavaFX.
     */
    public static void bindComponent(String fieldName, Object uiComponent, ViewState viewState) {
        if (uiComponent instanceof TextInputControl textInput) {
            createStringBind(fieldName, textInput.textProperty(), viewState);
        } else if (uiComponent instanceof Label label) {
            createStringBind(fieldName, label.textProperty(), viewState);
        } else if (uiComponent instanceof CheckBox checkBox) {
            // Exemplo futurdo de BooleanProperty
        } else if (uiComponent instanceof ToggleButton toggle) {
            // Exemplo futurdo de BooleanProperty
        }
    }

    private static void createStringBind(String fieldName, javafx.beans.property.StringProperty javafxProperty, ViewState viewState) {
        ThreadSafeStringProperty winterProperty = new ThreadSafeStringProperty(javafxProperty.get());

        // ❌ ANTES (Errado): bindBidirectional retorna VOID, não pode atribuir a uma variável
        // javafx.beans.binding.Binding<String> binding = Bindings.bindBidirectional(javafxProperty, winterProperty);

        // ✅ AGORA (Certo): Apenas executa o bind
        Bindings.bindBidirectional(javafxProperty, winterProperty);

        // Salva no nosso ViewModel oculto
        viewState.addProperty(fieldName, winterProperty);

        // Rastreia a ação de DESFAZER o bind para quando a View for destruída
        Runnable unbindAction = () -> Bindings.unbindBidirectional(javafxProperty, winterProperty);
        viewState.trackBinding(unbindAction);
    }
}