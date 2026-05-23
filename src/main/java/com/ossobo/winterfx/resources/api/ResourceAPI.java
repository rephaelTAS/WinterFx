package com.ossobo.winterfx.resources.api;

import com.ossobo.winterfx.di.annotations.*;
import com.ossobo.winterfx.di.scopes.enums.ScopeType;
import com.ossobo.winterfx.resources.descriptor.*;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.enums.ResourceType;
import com.ossobo.winterfx.resources.excecoes.ResourceNotFoundException;
import com.ossobo.winterfx.resources.excecoes.ResourceValidationException;
import com.ossobo.winterfx.resources.guard.ResourceGuard;
import com.ossobo.winterfx.resources.registry.ResourceRegistry;
import com.ossobo.winterfx.resources.resolver.ImageAnnotationResolver;
import com.ossobo.winterfx.resources.resolver.ResourceResolver;
import com.ossobo.winterfx.resources.resolver.ViewAnnotationResolver;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * 🌐 ResourceAPI v2.0
 *
 * Fachada unificada para o módulo de Resources.
 * Suporte completo a anotações @RegisterView e @RegisterImage.
 */
@Component
@ScopeAnnotation(ScopeType.SINGLETON)
public final class ResourceAPI {

    private static final Logger LOGGER = Logger.getLogger(ResourceAPI.class.getName());

    private final ResourceRegistry registry;
    private final ResourceResolver resolver;
    private final ResourceGuard guard;

    public ResourceAPI() {
        this.registry = new ResourceRegistry();
        this.resolver = new ResourceResolver(registry);
        this.guard = new ResourceGuard(registry);
        LOGGER.info("🌐 ResourceAPI v2.0 inicializada");
    }

    // ===== REGISTRO VIA ANOTAÇÕES =====

    /**
     * Registra uma view a partir de uma classe anotada com @RegisterView.
     */
    public ViewDescriptor registerFromAnnotatedClass(Class<?> annotatedClass) {
        ViewDescriptor descriptor = ViewAnnotationResolver.resolve(annotatedClass);
        registerView(descriptor);
        LOGGER.info("🪟 View registrada via anotação: " + descriptor.getId());
        return descriptor;
    }

    /**
     * Registra uma imagem a partir de uma anotação @RegisterImage.
     */
    public ImageDescriptor registerImageFromAnnotation(RegisterImage annotation) {
        ImageDescriptor descriptor = ImageAnnotationResolver.resolve(annotation);
        registerImage(descriptor);
        LOGGER.info("🖼️ Imagem registrada via anotação: " + descriptor.getId());
        return descriptor;
    }

    /**
     * Registra múltiplas imagens de uma classe anotada com @RegisterImages.
     */
    public List<ImageDescriptor> registerImagesFromClass(Class<?> clazz) {
        List<ImageDescriptor> descriptors = ImageAnnotationResolver.resolveFromClass(clazz);
        for (ImageDescriptor descriptor : descriptors) {
            registerImage(descriptor);
        }
        LOGGER.info("🖼️ " + descriptors.size() + " imagens registradas da classe: " + clazz.getSimpleName());
        return descriptors;
    }

    // ===== REGISTRO GENÉRICO =====

    public void register(ResourceDescriptor descriptor) {
        guard.validateForRegistration(descriptor);
        registry.register(descriptor);
        LOGGER.fine(() -> "📝 Recurso registrado: " + descriptor.getId());
    }

    public void registerAll(ResourceDescriptor... descriptors) {
        for (ResourceDescriptor descriptor : descriptors) {
            register(descriptor);
        }
    }

    public void unregister(String id) {
        registry.unregister(id);
        LOGGER.fine(() -> "🗑️ Recurso removido: " + id);
    }

    // ===== REGISTRO DE VIEW =====

    public void registerView(ViewDescriptor descriptor) {
        if (descriptor.getControllerClass() == null) {
            throw new ResourceValidationException("View requer controllerClass: " + descriptor.getId());
        }
        register(descriptor);
        LOGGER.fine(() -> "🪟 View registrada: " + descriptor.getId());
    }

    public void registerView(String id, URL fxmlUrl, Class<?> controllerClass, ResourceOrigin origin) {
        ViewDescriptor descriptor = ViewDescriptor.builder()
                .id(id)
                .fxmlUrl(fxmlUrl)
                .controllerClass(controllerClass)
                .origin(origin)
                .build();
        registerView(descriptor);
    }

    // ===== REGISTRO DE ALERTA =====

    public void registerAlert(ViewDescriptor descriptor) {
        if (descriptor.getAlertType() == null) {
            throw new ResourceValidationException("Alerta requer alertType: " + descriptor.getId());
        }
        if (descriptor.getModality() == null) {
            throw new ResourceValidationException("Alerta requer modality: " + descriptor.getId());
        }
        if (descriptor.getControllerClass() == null) {
            throw new ResourceValidationException("Alerta requer controllerClass: " + descriptor.getId());
        }
        register(descriptor);
        LOGGER.fine(() -> "⚠️ Alerta registrado: " + descriptor.getId());
    }

    // ===== REGISTRO DE IMAGEM =====

    public void registerImage(ImageDescriptor descriptor) {
        register(descriptor);
        LOGGER.fine(() -> "🖼️ Imagem registrada: " + descriptor.getId());
    }

    public void registerImage(String id, URL imageUrl, ResourceOrigin origin) {
        ImageDescriptor descriptor = ImageDescriptor.builder()
                .id(id)
                .url(imageUrl)
                .origin(origin)
                .build();
        registerImage(descriptor);
    }

    // ===== CONSULTA =====

    public Optional<ResourceDescriptor> find(String id) {
        return resolver.resolveDescriptor(id);
    }

    public ResourceDescriptor require(String id) {
        return find(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public boolean exists(String id) {
        return resolver.exists(id);
    }

    public boolean exists(String id, ResourceType type) {
        return resolver.exists(id, type);
    }

    // ===== ACESSO A URLS =====

    public URL getUrl(String id) {
        return resolver.resolveUrl(id);
    }

    public URL getViewUrl(String viewId) {
        return resolver.getViewUrl(viewId);
    }

    public URL getImageUrl(String imageId) {
        return resolver.getImageUrl(imageId);
    }

    public URL getCssUrl(String cssId) {
        return resolver.getCssUrl(cssId);
    }

    public URL getSoundUrl(String soundId) {
        return resolver.getSoundUrl(soundId);
    }

    public URL getAlertUrl(String alertId) {
        return resolver.getAlertUrl(alertId);
    }

    // ===== ACESSO A STREAMS =====

    public InputStream openStream(String id) {
        return resolver.resolveStream(id);
    }

    // ===== DESCRITORES TIPADOS =====

    public Optional<ViewDescriptor> getView(String id) {
        return resolver.resolveTyped(id, ViewDescriptor.class);
    }

    public Optional<ViewDescriptor> getViewDescriptor(String viewId) {
        return resolver.resolveTyped(viewId, ViewDescriptor.class)
                .filter(d -> d.getResourceType() == ResourceType.FXML);
    }

    public Optional<ViewDescriptor> getAlertDescriptor(String alertId) {
        return resolver.resolveTyped(alertId, ViewDescriptor.class)
                .filter(d -> d.getResourceType() == ResourceType.ALERT);
    }

    public Optional<ViewDescriptor> getViewDescriptor(String viewId, ResourceOrigin origin) {
        return resolver.resolveTyped(viewId, ViewDescriptor.class, origin);
    }

    public Optional<ImageDescriptor> getImageDescriptor(String imageId) {
        return resolver.resolveTyped(imageId, ImageDescriptor.class);
    }

    // ===== LISTAGEM =====

    public List<String> listAllIds() {
        return registry.findAll().stream()
                .map(ResourceDescriptor::getId)
                .toList();
    }

    public List<String> listIdsByType(ResourceType type) {
        return resolver.listIdsByType(type);
    }

    public List<ResourceDescriptor> listByType(ResourceType type) {
        return resolver.listByType(type);
    }

    public List<ViewDescriptor> listAllViews() {
        return resolver.listByType(ResourceType.FXML).stream()
                .filter(d -> d instanceof ViewDescriptor)
                .map(d -> (ViewDescriptor) d)
                .toList();
    }

    public List<ViewDescriptor> listAllAlerts() {
        return resolver.listByType(ResourceType.ALERT).stream()
                .filter(d -> d instanceof ViewDescriptor)
                .map(d -> (ViewDescriptor) d)
                .toList();
    }

    public List<ImageDescriptor> listAllImages() {
        return resolver.listByType(ResourceType.IMAGE).stream()
                .filter(d -> d instanceof ImageDescriptor)
                .map(d -> (ImageDescriptor) d)
                .toList();
    }

    // ===== ESTATÍSTICAS =====

    public int count() {
        return registry.count();
    }

    public long countByType(ResourceType type) {
        return registry.findAll().stream()
                .filter(d -> d.getResourceType() == type)
                .count();
    }

    public void clear() {
        registry.clear();
        LOGGER.warning("⚠️ Catálogo de recursos completamente limpo");
    }

    // ===== ACESSO AO REGISTRY (para StageManager) =====

    public ResourceRegistry getRegistry() {
        return registry;
    }

    public ResourceResolver getResolver() {
        return resolver;
    }

    @Override
    public String toString() {
        return String.format("ResourceAPI[total=%d, views=%d, alerts=%d, images=%d, css=%d, sounds=%d]",
                count(),
                countByType(ResourceType.FXML),
                countByType(ResourceType.ALERT),
                countByType(ResourceType.IMAGE),
                countByType(ResourceType.CSS),
                countByType(ResourceType.SOUND)
        );
    }
}