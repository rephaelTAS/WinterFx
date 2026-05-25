package com.ossobo.winterfx.resources.resolver;

import com.ossobo.winterfx.di.annotations.RegisterImage;
import com.ossobo.winterfx.di.annotations.RegisterImages;
import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ImageAnnotationResolver v2.2
 *
 * Resolvedor de anotações @RegisterImage para ImageDescriptor.
 * Faz a ponte entre os caminhos String da anotação e os URLs usados pelo ImageDescriptor.
 *
 * <p><b>🔥 ATUALIZAÇÃO v2.2:</b></p>
 * <ul>
 *   <li>Logs padronizados com java.util.logging</li>
 *   <li>Resolução de classpath de DUAS fontes:
 *     <ol>
 *       <li>ContextClassLoader (aplicação cliente)</li>
 *       <li>WinterFX ClassLoader (recursos internos do framework)</li>
 *     </ol>
 *   </li>
 * </ul>
 */
public final class ImageAnnotationResolver {

    private static final Logger LOGGER = Logger.getLogger(ImageAnnotationResolver.class.getName());

    private ImageAnnotationResolver() {}

    /**
     * Resolve uma anotação @RegisterImage em ImageDescriptor.
     */
    public static ImageDescriptor resolve(RegisterImage annotation) {
        URL url = resolveUrl(annotation.src(), annotation.id());

        ImageDescriptor descriptor = ImageDescriptor.builder()
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

        LOGGER.log(Level.FINE, "✅ ImageDescriptor criado: id={0}, src={1}",
                new Object[]{annotation.id(), annotation.src()});

        return descriptor;
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

        LOGGER.log(Level.FINE, "📦 {0} imagens resolvidas da classe {1}",
                new Object[]{descriptors.size(), clazz.getSimpleName()});

        return descriptors;
    }

    /**
     * 🔥 Resolve um caminho para URL.
     *
     * <p><b>Ordem de resolução:</b></p>
     * <ol>
     *   <li>ContextClassLoader (aplicação cliente) - ex: /images/logo.png</li>
     *   <li>WinterFX ClassLoader (recursos internos) - ex: /winterfx/icons/default.png</li>
     * </ol>
     *
     * @param path    Caminho do recurso
     * @param imageId ID da imagem (para mensagens de erro)
     * @return URL resolvida
     * @throws IllegalArgumentException se o recurso não for encontrado
     */
    private static URL resolveUrl(String path, String imageId) {
        // 1. PRIMEIRO: ContextClassLoader (aplicação cliente)
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);

        // 2. SEGUNDO: ClassLoader do WinterFX (recursos internos do framework)
        if (url == null) {
            url = ImageAnnotationResolver.class.getResource(path);
        }

        // 3. Se ainda não encontrou, lança exceção
        if (url == null) {
            String mensagem = String.format(
                    "Imagem não encontrada para '%s': %s", imageId, path
            );
            LOGGER.severe("❌ " + mensagem);
            throw new IllegalArgumentException(mensagem);
        }

        return url;
    }
}