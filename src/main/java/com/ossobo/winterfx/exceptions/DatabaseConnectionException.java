package com.ossobo.winterfx.exceptions;

// ===== EXCEÇÕES PERSONALIZADAS MINIMAIS =====

/**
 * Exceção para falhas de conexão com banco de dados
 * Nível: CRÍTICO
 */
public class DatabaseConnectionException extends RuntimeException {
    public DatabaseConnectionException(String message) {
        super(message);
    }

    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}


