package com.ossobo.winterfx.resources.resolver;

import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;
import com.ossobo.winterfx.resources.descriptor.ResourceDescriptor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.enums.ResourceType;
import com.ossobo.winterfx.resources.excecoes.ResourceNotFoundException;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * 🔍 ResourceResolver v2.1
 *
 * Camada de serviço para obtenção de recursos.
 * Transforma metadado em acesso prático sem interpretar o conteúdo.
 * É a ponte entre o catálogo (ResourceRegistry) e o uso real.
 *
 * <p><b>🔥 ATUALIZAÇÃO v2.1:</b></p>
 * <ul>
 *   <li>Adicionado método {@code resolveView(String)} para @RegisterView</li>
 *   <li>Adicionado método {@code resolveImage(String)} para @RegisterImage</li>
 *   <li>Adicionado método {@code resolveNotification(String)} para @RegisterNotification</li>
 *   <li>Logs padronizados com java.util.logging</li>
 * </ul>
 *
 * <pre>
 * Uso típico:
 *   URL url = resolver.resolveUrl("login-view");
 *   Optional&lt;ViewDescriptor&gt; view = resolver.resolveView("main");
 *   Optional&lt;ImageDescriptor&gt; img = resolver.resolveImage("logo");
 * </pre>
 */
public final class ResourceResolver {

    private final ResourceRegistry registry;

    public ResourceResolver(ResourceRegistry registry) {
        this.registry = registry;
    }

    // ===== RESOLUÇÃO DE URL =====

    public URL resolveUrl(String id) {
        return resolveDescriptor(id)
                .map(ResourceDescriptor::getUrl)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public URL resolveUrl(String id, ResourceType type) {
        return registry.findByIdAndType(id, type)
                .map(ResourceDescriptor::getUrl)
                .orElseThrow(() -> new ResourceNotFoundException(id, type));
    }

    // ===== RESOLUÇÃO DE DESCRIPTOR =====

    public Optional<ResourceDescriptor> resolveDescriptor(String id) {
        return registry.findById(id);
    }

    public Optional<ResourceDescriptor> resolveDescriptor(String id, ResourceType type) {
        return registry.findByIdAndType(id, type);
    }

    public Optional<ResourceDescriptor> resolveDescriptor(String id, ResourceType type,
                                                          ResourceOrigin origin) {
        return registry.findByIdAndType(id, type)
                .filter(descriptor -> descriptor.getOrigin() == origin);
    }

    // ===== RESOLUÇÃO COM CAST AUTOMÁTICO =====

    @SuppressWarnings("unchecked")
    public <T extends ResourceDescriptor> Optional<T> resolveTyped(String id,
                                                                   Class<T> descriptorClass) {
        return resolveDescriptor(id)
                .filter(descriptor -> descriptorClass.isAssignableFrom(descriptor.getClass()))
                .map(descriptor -> (T) descriptor);
    }

    @SuppressWarnings("unchecked")
    public <T extends ResourceDescriptor> Optional<T> resolveTyped(String id,
                                                                   Class<T> descriptorClass,
                                                                   ResourceOrigin origin) {
        return resolveDescriptor(id)
                .filter(descriptor -> descriptorClass.isAssignableFrom(descriptor.getClass()))
                .filter(descriptor -> descriptor.getOrigin() == origin)
                .map(descriptor -> (T) descriptor);
    }

    // ===== RESOLUÇÃO DE INPUTSTREAM =====

    public InputStream resolveStream(String id) throws ResourceNotFoundException {
        try {
            URL url = resolveUrl(id);
            return url.openStream();
        } catch (Exception e) {
            throw new ResourceNotFoundException(id, e);
        }
    }

    // ===== LISTAGEM E CONSULTA =====

    public List<String> listIdsByType(ResourceType type) {
        return registry.findAll().stream()
                .filter(descriptor -> descriptor.getResourceType() == type)
                .map(ResourceDescriptor::getId)
                .toList();
    }

    public List<ResourceDescriptor> listByType(ResourceType type) {
        return registry.findAll().stream()
                .filter(descriptor -> descriptor.getResourceType() == type)
                .toList();
    }

    public boolean exists(String id) {
        return registry.findById(id).isPresent();
    }

    public boolean exists(String id, ResourceType type) {
        return registry.findByIdAndType(id, type).isPresent();
    }

    // ===== MÉTODOS DE CONVENIÊNCIA POR TIPO =====

    /**
     * Resolve URL de uma view FXML.
     */
    public URL getViewUrl(String viewId) {
        return resolveUrl(viewId, ResourceType.FXML);
    }

    /**
     * Resolve URL de uma imagem.
     */
    public URL getImageUrl(String imageId) {
        return resolveUrl(imageId, ResourceType.IMAGE);
    }

    /**
     * Resolve URL de um CSS.
     */
    public URL getCssUrl(String cssId) {
        return resolveUrl(cssId, ResourceType.CSS);
    }

    /**
     * Resolve URL de um som.
     */
    public URL getSoundUrl(String soundId) {
        return resolveUrl(soundId, ResourceType.SOUND);
    }

    /**
     * Resolve URL de um alerta.
     */
    public URL getAlertUrl(String alertId) {
        return resolveUrl(alertId, ResourceType.ALERT);
    }

    // ===== 🔥 NOVOS MÉTODOS PARA ANOTAÇÕES =====

    /**
     * Resolve um ViewDescriptor (para @RegisterView).
     * Aceita FXML ou ALERT.
     */
    public Optional<ViewDescriptor> resolveView(String id) {
        return resolveTyped(id, ViewDescriptor.class)
                .filter(d -> d.getResourceType() == ResourceType.FXML
                        || d.getResourceType() == ResourceType.ALERT);
    }

    /**
     * Resolve um ViewDescriptor apenas para views FXML.
     */
    public Optional<ViewDescriptor> resolveFxmlView(String id) {
        return resolveTyped(id, ViewDescriptor.class)
                .filter(d -> d.getResourceType() == ResourceType.FXML);
    }

    /**
     * Resolve um ViewDescriptor apenas para alertas.
     */
    public Optional<ViewDescriptor> resolveAlert(String id) {
        return resolveTyped(id, ViewDescriptor.class)
                .filter(d -> d.getResourceType() == ResourceType.ALERT);
    }

    /**
     * Resolve um ImageDescriptor (para @RegisterImage).
     */
    public Optional<ImageDescriptor> resolveImage(String id) {
        return resolveTyped(id, ImageDescriptor.class);
    }

    /**
     * Resolve uma notificação (para @RegisterNotification).
     * Notificações são armazenadas como ViewDescriptor com tipo ALERT.
     */
    public Optional<ViewDescriptor> resolveNotification(String id) {
        return resolveTyped(id, ViewDescriptor.class)
                .filter(d -> d.getResourceType() == ResourceType.ALERT);
    }

    @Override
    public String toString() {
        return String.format("ResourceResolver[recursos=%d]", registry.count());
    }
}