package com.ossobo.winterfx.runtime;

/**
 * Exceção base do WinterFX.
 */
public class WinterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WinterException(String message) {
        super(message);
    }

    public WinterException(String message, Throwable cause) {
        super(message, cause);
    }
}