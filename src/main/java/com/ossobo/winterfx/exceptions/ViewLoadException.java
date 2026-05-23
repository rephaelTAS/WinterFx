package com.ossobo.winterfx.exceptions;

/**
 * ✅ MELHORIA: Hierarquia de exceções específicas
 */
public class ViewLoadException extends RuntimeException {
    private final String viewId;
    private final ErrorType errorType;

    public ViewLoadException(String viewId, String message, ErrorType errorType) {
        super(String.format("[%s] %s", viewId, message));
        this.viewId = viewId;
        this.errorType = errorType;
    }

    public enum ErrorType {
        RESOURCE_NOT_FOUND,
        CONTROLLER_INSTANTIATION_FAILED,
        FXML_SYNTAX_ERROR,
        IO_ERROR,
        UNKNOWN
    }
}
