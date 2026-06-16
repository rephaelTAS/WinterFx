/*
 * ResourceGuard v1.0
 *
 * Responsabilidade: validar duplicidade, tipo incompatível, id inválido e recurso ausente.
 * Entrada: descriptor ou id.
 * Saída: erro claro ou resultado válido.
 * Depende de: ResourceRegistry, ResourceType.
 */

package com.ossobo.winterfx.resources.guard;

import com.ossobo.winterfx.resources.descriptor.ResourceDescriptor;
import com.ossobo.winterfx.resources.enums.ResourceType;
import com.ossobo.winterfx.resources.excecoes.ResourceValidationException;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;

import java.util.Objects;

/**
 * 🛡️ ResourceGuard v1.0
 * <p>
 * Validador de consistência para recursos.
 * Garante que apenas recursos válidos sejam registrados e consumidos.
 * </p>
 *
 * <pre>
 * Uso típico:
 *   guard.validateForRegistration(descriptor);
 *   guard.validateExists("main-view", ResourceType.FXML);
 * </pre>
 */
public final class ResourceGuard {

    private final ResourceRegistry registry;

    /**
     * Construtor que recebe o registry como dependência.
     */
    public ResourceGuard(ResourceRegistry registry) {
        this.registry = registry;
    }

    // ===== VALIDAÇÃO DE REGISTRO =====

    /**
     * Valida um descriptor antes do registro.
     * Verifica: ID, URL, unicidade e consistência de tipo.
     *
     * @param descriptor Descriptor a ser validado
     * @throws ResourceValidationException Se validação falhar
     */
    public void validateForRegistration(ResourceDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "Descriptor não pode ser nulo");

        validateId(descriptor.getId());
        validateUrl(descriptor);
        validateUniqueness(descriptor);
        validateTypeConsistency(descriptor);
    }

    /**
     * Valida o formato do ID.
     * Aceita apenas: letras, números, ponto, underline e hífen.
     */
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

    /**
     * Valida se a URL não é nula.
     */
    private void validateUrl(ResourceDescriptor descriptor) {
        if (descriptor.getUrl() == null) {
            throw new ResourceValidationException(
                    "URL não pode ser nula para o recurso: " + descriptor.getId()
            );
        }
    }

    /**
     * Valida se o ID já não está registrado.
     */
    private void validateUniqueness(ResourceDescriptor descriptor) {
        registry.findById(descriptor.getId()).ifPresent(existing -> {
            throw new ResourceValidationException(
                    String.format("Recurso com ID '%s' já registrado (tipo: %s, origem: %s)",
                            descriptor.getId(), existing.getResourceType(), existing.getOrigin())
            );
        });
    }

    /**
     * Valida consistência entre extensão do arquivo e tipo declarado.
     * Apenas emite warning, não bloqueia o registro.
     */
    private void validateTypeConsistency(ResourceDescriptor descriptor) {
        String url = descriptor.getUrl().toString().toLowerCase();
        ResourceType declaredType = descriptor.getResourceType();
        ResourceType detectedType = detectTypeFromUrl(url);

        if (detectedType != ResourceType.UNKNOWN && detectedType != declaredType) {
        }
    }

    /**
     * Detecta o tipo do recurso baseado na extensão da URL.
     */
    private ResourceType detectTypeFromUrl(String url) {
        if (url.endsWith(".fxml")) return ResourceType.FXML;
        if (url.endsWith(".css")) return ResourceType.CSS;
        if (url.matches(".*\\.(png|jpg|jpeg|gif|bmp)$")) return ResourceType.IMAGE;
        if (url.matches(".*\\.(wav|mp3|aiff|m4a)$")) return ResourceType.SOUND;
        if (url.endsWith(".json")) return ResourceType.JSON;
        if (url.endsWith(".properties")) return ResourceType.PROPERTIES;
        return ResourceType.UNKNOWN;
    }

    // ===== VALIDAÇÃO DE CONSUMO =====

    /**
     * Valida existência de recurso antes do consumo.
     *
     * @param id Identificador do recurso
     * @param expectedType Tipo esperado
     * @throws ResourceValidationException Se recurso não existe ou tipo incompatível
     */
    public void validateExists(String id, ResourceType expectedType) {
        registry.findById(id).ifPresentOrElse(
                descriptor -> {
                    if (descriptor.getResourceType() != expectedType) {
                        throw new ResourceValidationException(
                                String.format("Recurso '%s' é do tipo %s, mas %s era esperado",
                                        id, descriptor.getResourceType(), expectedType)
                        );
                    }
                },
                () -> {
                    throw new ResourceValidationException("Recurso não encontrado: " + id);
                }
        );
    }

    /**
     * Valida se múltiplos recursos existem.
     *
     * @param ids Lista de identificadores
     * @param expectedType Tipo esperado para todos
     * @throws ResourceValidationException Se algum recurso não existe ou tipo incompatível
     */
    public void validateAllExist(java.util.Collection<String> ids, ResourceType expectedType) {
        for (String id : ids) {
            validateExists(id, expectedType);
        }
    }

    // ===== VERIFICAÇÕES (SEM LANÇAR EXCEÇÃO) =====

    /**
     * Verifica se um ID está disponível para registro.
     *
     * @param id Identificador a verificar
     * @return true se disponível
     */
    public boolean isIdAvailable(String id) {
        return registry.findById(id).isEmpty();
    }

    /**
     * Verifica se um descriptor é válido (sem lançar exceção).
     *
     * @param descriptor Descriptor a validar
     * @return true se válido
     */
    public boolean isValid(ResourceDescriptor descriptor) {
        try {
            validateForRegistration(descriptor);
            return true;
        } catch (ResourceValidationException e) {
            return false;
        }
    }

    // ===== SUGESTÕES =====

    /**
     * Sugere IDs alternativos baseados em um ID base.
     * Útil quando há colisão de nomes.
     *
     * @param baseId ID base desejado
     * @return ID sugerido com sufixo numérico
     */
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