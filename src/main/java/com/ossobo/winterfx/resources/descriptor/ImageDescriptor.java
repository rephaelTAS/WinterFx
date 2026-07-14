package com.ossobo.winterfx.resources.descriptor;

import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.enums.ResourceType;
import com.ossobo.winterfx.resources.enums.ViewAnimation;

import java.net.URL;
import java.util.Arrays;
import java.util.Objects;

/**
 * ImageDescriptor v2.1
 *
 * Descreve uma imagem ou ícone com metadados visuais.
 * Totalmente alinhado com a anotação @RegisterImage.
 *
 * <p><b>Mapeamento RegisterImage → ImageDescriptor:</b></p>
 * <ul>
 *   <li>{@code id()} → {@link #getId()}</li>
 *   <li>{@code src()} → {@link #getSrc()}</li>
 *   <li>{@code origin()} → {@link #getOrigin()}</li>
 *   <li>{@code imageType()} → {@link #getImageType()}</li>
 *   <li>{@code preferredWidth()} → {@link #getPreferredWidth()}</li>
 *   <li>{@code preferredHeight()} → {@link #getPreferredHeight()}</li>
 *   <li>{@code preserveRatio()} → {@link #isPreserveRatio()}</li>
 *   <li>{@code smooth()} → {@link #isSmooth()}</li>
 *   <li>{@code description()} → {@link #getDescription()}</li>
 *   <li>{@code tags()} → {@link #getTags()}</li>
 * </ul>
 */
public final class ImageDescriptor extends ResourceDescriptor {

    // ===== CAMPOS =====

    private final String src;              // ← @RegisterImage.src()
    private final ViewAnimation.ImageType imageType;     // ← @RegisterImage.imageType()
    private final double preferredWidth;   // ← @RegisterImage.preferredWidth()
    private final double preferredHeight;  // ← @RegisterImage.preferredHeight()
    private final boolean preserveRatio;   // ← @RegisterImage.preserveRatio()
    private final boolean smooth;          // ← @RegisterImage.smooth()
    private final String description;      // ← @RegisterImage.description()
    private final String[] tags;           // ← @RegisterImage.tags()
    private final ResourceOrigin origin;   // ← @RegisterImage.origin()

    // ===== CONSTRUTOR PRIVADO =====

    private ImageDescriptor(Builder builder) {
        super(builder.id, builder.url, ResourceType.IMAGE,
                Objects.requireNonNullElse(builder.origin, ResourceOrigin.APPLICATION));

        this.src = builder.src;
        this.imageType = Objects.requireNonNullElse(builder.imageType, ViewAnimation.ImageType.IMAGE);
        this.preferredWidth = builder.preferredWidth;
        this.preferredHeight = builder.preferredHeight;
        this.preserveRatio = builder.preserveRatio;
        this.smooth = builder.smooth;
        this.description = builder.description != null ? builder.description : "";
        this.tags = builder.tags != null ? builder.tags.clone() : new String[0];
        this.origin = builder.origin;
    }

    // ===== GETTERS =====

    /**
     * Caminho da imagem (String) - alinhado com @RegisterImage.src()
     */
    public String getSrc() { return src; }

    public ViewAnimation.ImageType getImageType() { return imageType; }
    public double getPreferredWidth() { return preferredWidth; }
    public double getPreferredHeight() { return preferredHeight; }
    public boolean isPreserveRatio() { return preserveRatio; }
    public boolean isSmooth() { return smooth; }
    public String getDescription() { return description; }
    public String[] getTags() { return tags.clone(); }
    public ResourceOrigin getOrigin() { return origin; }

    /**
     * URL da imagem - resolvida a partir do src.
     */
    public URL getImageUrl() { return getUrl(); }

    // ===== UTILITY =====

    @Override
    public String toString() {
        return "ImageDescriptor{" +
                "id='" + getId() + '\'' +
                ", src='" + src + '\'' +
                ", imageType=" + imageType +
                ", preferredWidth=" + preferredWidth +
                ", preferredHeight=" + preferredHeight +
                ", preserveRatio=" + preserveRatio +
                ", smooth=" + smooth +
                ", description='" + description + '\'' +
                ", tags=" + Arrays.toString(tags) +
                ", origin=" + origin +
                ", url=" + getUrl() +
                '}';
    }

    // ===== BUILDER =====

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private URL url;
        private String src;                    // ← NOVO: caminho String
        private ResourceOrigin origin = ResourceOrigin.APPLICATION;
        private ViewAnimation.ImageType imageType = ViewAnimation.ImageType.IMAGE;
        private double preferredWidth = -1;
        private double preferredHeight = -1;
        private boolean preserveRatio = true;
        private boolean smooth = true;
        private String description;
        private String[] tags;

        public Builder id(String id) { this.id = id; return this; }
        public Builder url(URL url) { this.url = url; return this; }

        /**
         * Caminho da imagem (String) - alinhado com @RegisterImage.src()
         */
        public Builder src(String src) { this.src = src; return this; }

        public Builder origin(ResourceOrigin origin) { this.origin = origin; return this; }
        public Builder imageType(ViewAnimation.ImageType type) { this.imageType = type; return this; }
        public Builder preferredWidth(double width) { this.preferredWidth = width; return this; }
        public Builder preferredHeight(double height) { this.preferredHeight = height; return this; }
        public Builder preserveRatio(boolean preserve) { this.preserveRatio = preserve; return this; }
        public Builder smooth(boolean smooth) { this.smooth = smooth; return this; }
        public Builder description(String desc) { this.description = desc; return this; }
        public Builder tags(String... tags) { this.tags = tags; return this; }

        public ImageDescriptor build() {
            Objects.requireNonNull(id, "id é obrigatório");
            Objects.requireNonNull(url, "url é obrigatório");
            // src é opcional mas recomendado para debug
            return new ImageDescriptor(this);
        }
    }
}