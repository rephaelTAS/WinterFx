package com.ossobo.winterfx.resources.resolver;

import com.ossobo.winterfx.di.annotations.RegisterImage;
import com.ossobo.winterfx.di.annotations.RegisterImages;
import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * ImageAnnotationResolver v2.0
 *
 * Resolvedor de anotações @RegisterImage para ImageDescriptor.
 * Faz a ponte entre os caminhos String da anotação e os URLs usados pelo ImageDescriptor.
 */
public final class ImageAnnotationResolver {

    private ImageAnnotationResolver() {}

    /**
     * Resolve uma anotação @RegisterImage em ImageDescriptor.
     */
    public static ImageDescriptor resolve(RegisterImage annotation) {
        URL url = resolveUrl(annotation.src(), annotation.id());

        return ImageDescriptor.builder()
                .id(annotation.id())
                .url(url)
                .origin(annotation.origin())
                .imageType(annotation.imageType())
                .preferredWidth(annotation.preferredWidth())
                .preferredHeight(annotation.preferredHeight())
                .preserveRatio(annotation.preserveRatio())
                .smooth(annotation.smooth())
                .description(annotation.description())
                .tags(annotation.tags())
                .build();
    }

    /**
     * Resolve múltiplas anotações @RegisterImage de uma classe.
     */
    public static List<ImageDescriptor> resolveFromClass(Class<?> clazz) {
        List<ImageDescriptor> descriptors = new ArrayList<>();

        // Verifica @RegisterImages (container)
        RegisterImages registerImages = clazz.getAnnotation(RegisterImages.class);
        if (registerImages != null) {
            for (RegisterImage ann : registerImages.value()) {
                descriptors.add(resolve(ann));
            }
        }

        // Verifica @RegisterImage individual
        RegisterImage registerImage = clazz.getAnnotation(RegisterImage.class);
        if (registerImage != null) {
            descriptors.add(resolve(registerImage));
        }

        return descriptors;
    }

    private static URL resolveUrl(String path, String imageId) {
        URL url = ImageAnnotationResolver.class.getResource(path);
        if (url == null) {
            throw new IllegalArgumentException(
                    "Imagem não encontrada para '" + imageId + "': " + path
            );
        }
        return url;
    }
}