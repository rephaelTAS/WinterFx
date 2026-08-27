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
 * 🔍 ResourceResolver v5.0
 *
 * Camada de serviço para obtenção de recursos.
 * Usa interface Record-style (url(), id(), resourceType(), origin()).
 */
public final class ResourceResolver {

    private final ResourceRegistry registry;

    public ResourceResolver(ResourceRegistry registry) {
        this.registry = registry;
    }

    public URL resolveUrl(String id) {
        return resolveDescriptor(id)
                .map(ResourceDescriptor::url)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public URL resolveUrl(String id, ResourceType type) {
        return registry.findByIdAndType(id, type)
                .map(ResourceDescriptor::url)
                .orElseThrow(() -> new ResourceNotFoundException(id, type));
    }

    public Optional<ResourceDescriptor> resolveDescriptor(String id) {
        return registry.findById(id);
    }

    public Optional<ResourceDescriptor> resolveDescriptor(String id, ResourceType type) {
        return registry.findByIdAndType(id, type);
    }

    public Optional<ResourceDescriptor> resolveDescriptor(String id, ResourceType type,
                                                          ResourceOrigin origin) {
        return registry.findByIdAndType(id, type)
                .filter(descriptor -> descriptor.origin() == origin);
    }

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
                .filter(descriptor -> descriptor.origin() == origin)
                .map(descriptor -> (T) descriptor);
    }

    public InputStream resolveStream(String id) throws ResourceNotFoundException {
        try {
            var url = resolveUrl(id);
            return url.openStream();
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceNotFoundException(id, e);
        }
    }

    public List<String> listIdsByType(ResourceType type) {
        return registry.findAll().stream()
                .filter(descriptor -> descriptor.resourceType() == type)
                .map(ResourceDescriptor::id)
                .toList();
    }

    public List<ResourceDescriptor> listByType(ResourceType type) {
        return registry.findAll().stream()
                .filter(descriptor -> descriptor.resourceType() == type)
                .toList();
    }

    public boolean exists(String id) {
        return registry.findById(id).isPresent();
    }

    public boolean exists(String id, ResourceType type) {
        return registry.findByIdAndType(id, type).isPresent();
    }

    public URL getViewUrl(String viewId) {
        return resolveUrl(viewId, ResourceType.FXML);
    }

    public URL getImageUrl(String imageId) {
        return resolveUrl(imageId, ResourceType.IMAGE);
    }

    public URL getCssUrl(String cssId) {
        return resolveUrl(cssId, ResourceType.CSS);
    }

    public URL getSoundUrl(String soundId) {
        return resolveUrl(soundId, ResourceType.SOUND);
    }

    public URL getAlertUrl(String alertId) {
        return resolveUrl(alertId, ResourceType.ALERT);
    }

    public Optional<ViewDescriptor> resolveView(String id) {
        return resolveTyped(id, ViewDescriptor.class)
                .filter(d -> d.resourceType() == ResourceType.FXML
                        || d.resourceType() == ResourceType.ALERT);
    }

    public Optional<ViewDescriptor> resolveFxmlView(String id) {
        return resolveTyped(id, ViewDescriptor.class)
                .filter(d -> d.resourceType() == ResourceType.FXML);
    }

    public Optional<ViewDescriptor> resolveAlert(String id) {
        return resolveTyped(id, ViewDescriptor.class)
                .filter(d -> d.resourceType() == ResourceType.ALERT);
    }

    public Optional<ImageDescriptor> resolveImage(String id) {
        return resolveTyped(id, ImageDescriptor.class);
    }

    public Optional<ViewDescriptor> resolveNotification(String id) {
        return resolveTyped(id, ViewDescriptor.class)
                .filter(d -> d.resourceType() == ResourceType.ALERT);
    }

    @Override
    public String toString() {
        return String.format("ResourceResolver[recursos=%d]", registry.count());
    }
}