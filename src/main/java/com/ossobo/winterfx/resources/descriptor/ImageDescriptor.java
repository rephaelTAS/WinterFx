package com.ossobo.winterfx.resources.descriptor;

import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.enums.ResourceType;
import com.ossobo.winterfx.resources.enums.ViewAnimation;

import java.net.URL;
import java.util.List;
import java.util.Objects;

/**
 * ImageDescriptor v5.0 - Record puro
 *
 * Descreve uma imagem ou ícone com metadados visuais.
 * Implementa ResourceDescriptor (interface estilo Record).
 */
public record ImageDescriptor(
        String id,
        URL url,
        String src,
        ViewAnimation.ImageType imageType,
        double preferredWidth,
        double preferredHeight,
        boolean preserveRatio,
        boolean smooth,
        String description,
        List<String> tags,
        ResourceOrigin origin
) implements ResourceDescriptor {

    /**
     * Construtor compacto para validação e imutabilidade profunda.
     */
    public ImageDescriptor {
        Objects.requireNonNull(id, "id é obrigatório");
        Objects.requireNonNull(url, "url é obrigatório");
        Objects.requireNonNull(src, "src é obrigatório");

        imageType = Objects.requireNonNullElse(imageType, ViewAnimation.ImageType.IMAGE);
        description = description != null ? description : "";
        tags = tags != null ? List.copyOf(tags) : List.of();
        origin = Objects.requireNonNullElse(origin, ResourceOrigin.APPLICATION);
    }

    @Override
    public ResourceType resourceType() { return ResourceType.IMAGE; }

    /**
     * Alias legado para compatibilidade.
     */
    public URL getImageUrl() { return url; }

    @Override
    public String toString() {
        return "ImageDescriptor{" +
                "id='" + id + '\'' +
                ", src='" + src + '\'' +
                ", imageType=" + imageType +
                ", preferredWidth=" + preferredWidth +
                ", preferredHeight=" + preferredHeight +
                ", preserveRatio=" + preserveRatio +
                ", smooth=" + smooth +
                ", description='" + description + '\'' +
                ", tags=" + tags +
                ", origin=" + origin +
                ", url=" + url +
                '}';
    }

    /**
     * Builder fluente para ImageDescriptor.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private URL url;
        private String src;
        private ResourceOrigin origin = ResourceOrigin.APPLICATION;
        private ViewAnimation.ImageType imageType = ViewAnimation.ImageType.IMAGE;
        private double preferredWidth = -1;
        private double preferredHeight = -1;
        private boolean preserveRatio = true;
        private boolean smooth = true;
        private String description;
        private List<String> tags;

        public Builder id(String id) { this.id = id; return this; }
        public Builder url(URL url) { this.url = url; return this; }
        public Builder src(String src) { this.src = src; return this; }
        public Builder origin(ResourceOrigin origin) { this.origin = origin; return this; }
        public Builder imageType(ViewAnimation.ImageType type) { this.imageType = type; return this; }
        public Builder preferredWidth(double width) { this.preferredWidth = width; return this; }
        public Builder preferredHeight(double height) { this.preferredHeight = height; return this; }
        public Builder preserveRatio(boolean preserve) { this.preserveRatio = preserve; return this; }
        public Builder smooth(boolean smooth) { this.smooth = smooth; return this; }
        public Builder description(String desc) { this.description = desc; return this; }

        // Sobrecarga varargs para facilitar uso no Scanner
        public Builder tags(String... tags) {
            this.tags = tags != null ? List.of(tags) : List.of();
            return this;
        }

        // Sobrecarga para lista já criada
        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public ImageDescriptor build() {
            Objects.requireNonNull(id, "id é obrigatório");
            Objects.requireNonNull(url, "url é obrigatório");
            return new ImageDescriptor(id, url, src, imageType, preferredWidth, preferredHeight,
                    preserveRatio, smooth, description, tags, origin);
        }
    }
}