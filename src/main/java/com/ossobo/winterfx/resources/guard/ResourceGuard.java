package com.ossobo.winterfx.resources.guard;

import com.ossobo.winterfx.resources.descriptor.ResourceDescriptor;
import com.ossobo.winterfx.resources.enums.ResourceType;
import com.ossobo.winterfx.resources.excecoes.ResourceValidationException;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;

import java.util.Collection;
import java.util.Objects;

/**
 * 🛡️ ResourceGuard v3.0
 *
 * Validador de consistência para recursos.
 * Usa interface Record-style.
 */
public final class ResourceGuard {

    private static final System.Logger LOGGER = System.getLogger(ResourceGuard.class.getName());

    private final ResourceRegistry registry;

    public ResourceGuard(ResourceRegistry registry) {
        this.registry = registry;
    }

    public void validateForRegistration(ResourceDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "Descriptor não pode ser nulo");

        validateId(descriptor.id());
        validateUrl(descriptor);
        validateUniqueness(descriptor);
        validateTypeConsistency(descriptor);
    }

    private void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new ResourceValidationException("ID do recurso não pode ser vazio");
        }

        if (!id.matches("^[a-zA-Z0-9._-]+$")) {
            throw new ResourceValidationException(
                    "ID inválido: '" + id + "'. Use apenas letras, números, ponto, underline e hífen"
            );
        }
    }

    private void validateUrl(ResourceDescriptor descriptor) {
        if (descriptor.url() == null) {
            throw new ResourceValidationException(
                    "URL não pode ser nula para o recurso: " + descriptor.id()
            );
        }
    }

    private void validateUniqueness(ResourceDescriptor descriptor) {
        registry.findById(descriptor.id()).ifPresent(existing -> {
            throw new ResourceValidationException(
                    String.format("Recurso com ID '%s' já registrado (tipo: %s, origem: %s)",
                            descriptor.id(), existing.resourceType(), existing.origin())
            );
        });
    }

    private void validateTypeConsistency(ResourceDescriptor descriptor) {
        var url = descriptor.url().toString().toLowerCase();
        var declaredType = descriptor.resourceType();
        var detectedType = detectTypeFromUrl(url);

        if (detectedType != ResourceType.UNKNOWN && detectedType != declaredType) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Tipo declarado ({0}) diverge da extensão detectada ({1}) no recurso: {2}",
                    declaredType, detectedType, descriptor.id());
        }
    }

    private ResourceType detectTypeFromUrl(String url) {
        if (url.endsWith(".fxml")) return ResourceType.FXML;
        if (url.endsWith(".css")) return ResourceType.CSS;
        if (url.matches(".*\\.(png|jpg|jpeg|gif|bmp)$")) return ResourceType.IMAGE;
        if (url.matches(".*\\.(wav|mp3|aiff|m4a)$")) return ResourceType.SOUND;
        if (url.endsWith(".json")) return ResourceType.JSON;
        if (url.endsWith(".properties")) return ResourceType.PROPERTIES;
        return ResourceType.UNKNOWN;
    }

    public void validateExists(String id, ResourceType expectedType) {
        registry.findById(id).ifPresentOrElse(
                descriptor -> {
                    if (descriptor.resourceType() != expectedType) {
                        throw new ResourceValidationException(
                                String.format("Recurso '%s' é do tipo %s, mas %s era esperado",
                                        id, descriptor.resourceType(), expectedType)
                        );
                    }
                },
                () -> {
                    throw new ResourceValidationException("Recurso não encontrado: " + id);
                }
        );
    }

    public void validateAllExist(Collection<String> ids, ResourceType expectedType) {
        for (String id : ids) {
            validateExists(id, expectedType);
        }
    }

    public boolean isIdAvailable(String id) {
        return registry.findById(id).isEmpty();
    }

    public boolean isValid(ResourceDescriptor descriptor) {
        try {
            validateForRegistration(descriptor);
            return true;
        } catch (ResourceValidationException e) {
            return false;
        }
    }

    public String suggestAlternativeId(String baseId) {
        if (isIdAvailable(baseId)) {
            return baseId;
        }

        int counter = 1;
        String suggested;
        do {
            suggested = baseId + "-" + counter;
            counter++;
        } while (!isIdAvailable(suggested) && counter < 100);

        return suggested;
    }
}