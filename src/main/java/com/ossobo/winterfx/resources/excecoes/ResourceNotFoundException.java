/*
 * ResourceNotFoundException v1.0
 *
 * Exceção lançada quando um recurso não é encontrado no catálogo.
 * Parte do módulo Resource - NexusFX.
 */

package com.ossobo.winterfx.resources.excecoes;

import com.ossobo.winterfx.resources.enums.ResourceType;

/**
 * 🚫 ResourceNotFoundException v1.0
 * <p>
 * Exceção específica para recursos não encontrados.
 * Fornece informações detalhadas para depuração.
 * </p>
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceId;
    private final ResourceType expectedType;

    /**
     * Construtor para recurso não encontrado por ID.
     */
    public ResourceNotFoundException(String resourceId) {
        super("Recurso não encontrado: '" + resourceId + "'");
        this.resourceId = resourceId;
        this.expectedType = null;
    }

    /**
     * Construtor para recurso não encontrado por ID e tipo.
     */
    public ResourceNotFoundException(String resourceId, ResourceType expectedType) {
        super(String.format("Recurso '%s' do tipo %s não encontrado no catálogo",
                resourceId, expectedType));
        this.resourceId = resourceId;
        this.expectedType = expectedType;
    }

    /**
     * Construtor para falha de acesso ao recurso.
     */
    public ResourceNotFoundException(String resourceId, Throwable cause) {
        super("Falha ao acessar recurso: '" + resourceId + "' - " + cause.getMessage(), cause);
        this.resourceId = resourceId;
        this.expectedType = null;
    }

    public String getResourceId() {
        return resourceId;
    }

    public ResourceType getExpectedType() {
        return expectedType;
    }
}
