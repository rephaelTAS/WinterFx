package com.ossobo.winterfx.resources.descriptor;

import com.ossobo.winterfx.imagemanager.enums.ImageType;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.enums.ResourceType;

import java.net.URL;
import java.util.Objects;

/**
 * ImageDescriptor v2.0
 *
 * Descreve uma imagem ou ícone com metadados visuais.
 * Segue o mesmo padrão do ViewDescriptor com Builder.
 */
public final class ImageDescriptor extends ResourceDescriptor {



    // ===== CAMPOS =====

    private final ImageType imageType;
    private final double preferredWidth;
    private final double preferredHeight;
    private final boolean preserveRatio;
    private final boolean smooth;
    private final String description;
    private final String[] tags;

    // ===== CONSTRUTOR PRIVADO =====

    private ImageDescriptor(Builder builder) {
        super(builder.id, builder.url, ResourceType.IMAGE,
                Objects.requireNonNullElse(builder.origin, ResourceOrigin.APPLICATION));

        this.imageType = Objects.requireNonNullElse(builder.imageType, ImageType.IMAGE);
        this.preferredWidth = builder.preferredWidth;
        this.preferredHeight = builder.preferredHeight;
        this.preserveRatio = builder.preserveRatio;
        this.smooth = builder.smooth;
        this.description = builder.description != null ? builder.description : "";
        this.tags = builder.tags != null ? builder.tags.clone() : new String[0];
    }

    // ===== GETTERS =====

    public ImageType getImageType()         { return imageType; }
    public double getPreferredWidth()       { return preferredWidth; }
    public double getPreferredHeight()      { return preferredHeight; }
    public boolean isPreserveRatio()        { return preserveRatio; }
    public boolean isSmooth()               { return smooth; }
    public String getDescription()          { return description; }
    public String[] getTags()               { return tags.clone(); }
    public URL getImageUrl()                { return getUrl(); }

    // ===== BUILDER =====

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private URL url;
        private ResourceOrigin origin = ResourceOrigin.APPLICATION;
        private ImageType imageType = ImageType.IMAGE;
        private double preferredWidth = -1;
        private double preferredHeight = -1;
        private boolean preserveRatio = true;
        private boolean smooth = true;
        private String description;
        private String[] tags;

        public Builder id(String id)                     { this.id = id; return this; }
        public Builder url(URL url)                      { this.url = url; return this; }
        public Builder origin(ResourceOrigin origin)     { this.origin = origin; return this; }
        public Builder imageType(ImageType type)         { this.imageType = type; return this; }
        public Builder preferredWidth(double width)      { this.preferredWidth = width; return this; }
        public Builder preferredHeight(double height)    { this.preferredHeight = height; return this; }
        public Builder preserveRatio(boolean preserve)   { this.preserveRatio = preserve; return this; }
        public Builder smooth(boolean smooth)            { this.smooth = smooth; return this; }
        public Builder description(String desc)          { this.description = desc; return this; }
        public Builder tags(String... tags)              { this.tags = tags; return this; }

        public ImageDescriptor build() {
            Objects.requireNonNull(id, "id é obrigatório");
            Objects.requireNonNull(url, "url é obrigatório");
            return new ImageDescriptor(this);
        }
    }
}