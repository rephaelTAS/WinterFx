package com.ossobo.winterfx.resources.descriptor;

import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.enums.ResourceType;

import java.net.URL;
import java.util.Objects;

/**
 * Classe base para todos os descritores de recursos do WinterFX.
 *
 * <p>Contém os atributos comuns a todos os tipos de recursos:
 * ID único, URL do recurso, tipo e origem.</p>
 */
public abstract class ResourceDescriptor {

    private final String id;
    private final URL url;
    private final ResourceType resourceType;
    private final ResourceOrigin origin;

    protected ResourceDescriptor(String id, URL url, ResourceType resourceType,
                                 ResourceOrigin origin) {
        this.id = Objects.requireNonNull(id, "id é obrigatório");
        this.url = Objects.requireNonNull(url, "url é obrigatório");
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType é obrigatório");
        this.origin = Objects.requireNonNullElse(origin, ResourceOrigin.APPLICATION);
    }

    public String getId() { return id; }
    public URL getUrl() { return url; }
    public ResourceType getResourceType() { return resourceType; }
    public ResourceOrigin getOrigin() { return origin; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceDescriptor that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id='" + id + "', url=" + url + '}';
    }
}