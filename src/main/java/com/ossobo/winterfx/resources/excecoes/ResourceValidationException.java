/*
 * ResourceValidationException v1.0
 *
 * Exceção lançada quando a validação de um recurso falha.
 * Parte do módulo Resource - NexusFX.
 */

package com.ossobo.winterfx.resources.excecoes;

/**
 * 🛡️ ResourceValidationException v1.0
 * <p>
 * Exceção específica para falhas de validação de recursos.
 * Usada pelo ResourceGuard para rejeitar registros inválidos.
 * </p>
 */
public class ResourceValidationException extends RuntimeException {

    /**
     * Construtor com mensagem simples.
     */
    public ResourceValidationException(String message) {
        super(message);
    }

    /**
     * Construtor com mensagem e causa.
     */
    public ResourceValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
