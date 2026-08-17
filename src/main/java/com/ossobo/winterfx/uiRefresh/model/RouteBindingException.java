package com.ossobo.winterfx.uiRefresh.model;

public class RouteBindingException extends RuntimeException {

    public RouteBindingException(String message) {
        super(message); // Importante: repassa a mensagem para a classe pai
    }

    // Opcional, mas recomendado para manter o rastro da exceção original
    public RouteBindingException(String message, Throwable cause) {
        super(message, cause);
    }
}