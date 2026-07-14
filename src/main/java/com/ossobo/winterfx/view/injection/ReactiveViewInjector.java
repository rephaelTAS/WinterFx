package com.ossobo.winterfx.view.injection;

import com.ossobo.winterfx.view.binding.BindEngine;
import javafx.fxml.FXML;
import javafx.scene.Node;

import java.lang.reflect.Field;

/**
 * Injetor reativo pós-FXML.
 * Ele não injeta a view do zero, ele INTERCEPTA o que o JavaFX já injetou
 * e aplica a camada de reatividade (Thread-Safe MVVM) por cima.
 */
public class ReactiveViewInjector {

    private final ViewState viewState;

    public ReactiveViewInjector(ViewState viewState) {
        this.viewState = viewState;
    }

    /**
     * Lê os campos @FXML já injetados pelo FXMLLoader e os torna reativos.
     */
    public void injectReactiveState(Object controller, Node rootElement) {
        Class<?> clazz = controller.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            // Só nos importamos com campos anotados com @FXML (injetados pelo JavaFX)
            // Se a pessoa usou @InjectView para composição, o ViewCompositionInjector cuida disso.
            if (!field.isAnnotationPresent(FXML.class)) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object uiComponent = field.get(controller); // Pega o que o JavaFX injetou!

                if (uiComponent != null) {
                    // Manda para o BindEngine criar a ThreadSafeProperty oculta
                    BindEngine.bindComponent(field.getName(), uiComponent, viewState);
                }

            } catch (IllegalAccessException e) {
                // Falha silenciosa para não quebrar a aplicação por causa de um bind
                System.err.println("WinterFx MVVM: Falha ao criar bind reativo para o campo " + field.getName());
            }
        }
    }

    public ViewState getViewState() {
        return viewState;
    }
}