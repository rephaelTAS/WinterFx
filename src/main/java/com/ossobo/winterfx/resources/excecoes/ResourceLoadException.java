/*
 * ResourceLoadException v1.0
 *
 * Exceção lançada quando o carregamento de um recurso falha.
 * Usada pelo ResourceLoader.
 */

package com.ossobo.winterfx.resources.excecoes;

/**
 * 🔧 ResourceLoadException v1.0
 * <p>
 * Exceção específica para falhas de carregamento/interpretação de recursos.
 * </p>
 */
public class ResourceLoadException extends RuntimeException {

    private final String resourceId;
    private final String resourceType;

    public ResourceLoadException(String resourceId, String resourceType, Throwable cause) {
        super(String.format("Falha ao carregar %s '%s': %s",
                resourceType, resourceId, cause.getMessage()), cause);
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }

    public ResourceLoadException(String resourceId, String resourceType, String message) {
        super(String.format("Falha ao carregar %s '%s': %s",
                resourceType, resourceId, message));
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }
}
