package com.ossobo.winterfx.view.injection;

import javafx.beans.value.ObservableValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa o estado reativo (ViewModel oculto) de uma View gerenciada pelo WinterFx.
 * Armazena as propriedades sincronizadas com os componentes visuais.
 */
public class ViewState {

    // Mapa que liga o nome do campo (ex: "lblStatus") à sua propriedade reativa
    private final Map<String, ObservableValue<?>> reactiveProperties = new ConcurrentHashMap<>();

    // Lista de ações de limpeza (unbind/dispose) ativos para evitar Memory Leak
    private final List<Runnable> cleanupActions = new ArrayList<>();

    public void addProperty(String fieldName, ObservableValue<?> property) {
        reactiveProperties.put(fieldName, property);
    }

    public ObservableValue<?> getProperty(String fieldName) {
        return reactiveProperties.get(fieldName);
    }

    /**
     * Rastreia uma ação de limpeza (como unbind ou dispose) para ser executada
     * quando a view for destruída.
     */
    public void trackBinding(Runnable cleanupAction) {
        cleanupActions.add(cleanupAction);
    }

    /**
     * CHAMADO OBRIGATORIAMENTE quando a tela for fechada.
     * Limpa os bindings para que a tela seja coletada pelo Garbage Collector.
     */
    public void destroy() {
        cleanupActions.forEach(Runnable::run);
        cleanupActions.clear();
        reactiveProperties.clear();
    }
}