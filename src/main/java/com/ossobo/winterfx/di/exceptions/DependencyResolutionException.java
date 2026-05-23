package com.ossobo.winterfx.di.exceptions;

public class DependencyResolutionException extends RuntimeException {
    public DependencyResolutionException(String message) {
        super("Erro ao resolver dependência: " + message);
    }

    public DependencyResolutionException(String message, Throwable cause) {
        super("Erro ao resolver dependência: " + message, cause);
    }
}