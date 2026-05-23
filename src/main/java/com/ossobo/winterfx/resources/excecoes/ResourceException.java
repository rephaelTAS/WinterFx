package com.ossobo.winterfx.resources.excecoes;

/**
 * Exceção específica para erros de recurso.
 */
public class ResourceException extends RuntimeException {

    public ResourceException(String message) {
        super(message);
    }

    public ResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
