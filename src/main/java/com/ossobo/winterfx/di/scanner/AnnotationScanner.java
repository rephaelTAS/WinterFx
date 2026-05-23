package com.ossobo.winterfx.di.scanner;

import com.ossobo.winterfx.di.annotations.RegisterImage;
import com.ossobo.winterfx.di.annotations.RegisterImages;
import com.ossobo.winterfx.di.annotations.RegisterView;
import com.ossobo.winterfx.resources.api.ResourceAPI;
import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.resolver.ImageAnnotationResolver;
import com.ossobo.winterfx.resources.resolver.ViewAnnotationResolver;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 🏷️ AnnotationScanner v1.0
 *
 * Scanner especializado para anotações de recursos:
 * - @RegisterView → ViewDescriptor
 * - @RegisterImage / @RegisterImages → ImageDescriptor
 *
 * <p>Fluxo:
 * <ol>
 *   <li>ClassGraph descobre classes com @RegisterView e @RegisterImage</li>
 *   <li>Resolve as anotações para descritores (usando os resolvers)</li>
 *   <li>Registra no ResourceAPI</li>
 * </ol>
 *
 * <p>Totalmente integrado com o sistema de injeção por anotações.</p>
 */
public final class AnnotationScanner {

    private static final Logger LOGGER = Logger.getLogger(AnnotationScanner.class.getName());

    private final ResourceAPI resourceAPI;
    private final String[] basePackages;

    // Estatísticas
    private int viewsFound = 0;
    private int imagesFound = 0;

    /**
     * Construtor principal.
     *
     * @param resourceAPI  API de recursos para registro
     * @param basePackages Pacotes a escanear
     */
    public AnnotationScanner(ResourceAPI resourceAPI, String... basePackages) {
        this.resourceAPI = resourceAPI;
        this.basePackages = (basePackages != null && basePackages.length > 0)
                ? basePackages
                : new String[]{""};
    }

    /**
     * Executa o scan completo:
     * 1. Descobre @RegisterView
     * 2. Descobre @RegisterImage / @RegisterImages
     * 3. Registra tudo no ResourceAPI
     */
    public void scanAndRegister() {
        LOGGER.log(Level.INFO, "🏷️ Iniciando scan de recursos nos pacotes: {0}",
                String.join(", ", basePackages));

        long startTime = System.currentTimeMillis();

        try (ScanResult result = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .acceptPackages(basePackages)
                .scan()) {

            // ===== SCAN DE @RegisterView =====
            scanViews(result);

            // ===== SCAN DE @RegisterImage =====
            scanImages(result);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Falha no scan de recursos: {0}", e.getMessage());
            throw new RuntimeException("Falha no escaneamento de recursos", e);
        }

        long duration = System.currentTimeMillis() - startTime;
        printSummary(duration);
    }

    // =============================================
    // SCAN DE VIEWS (@RegisterView)
    // =============================================

    /**
     * Escaneia e registra views anotadas com @RegisterView.
     */
    private void scanViews(ScanResult result) {
        Set<String> viewClassNames = result.getClassesWithAnnotation(RegisterView.class).getNames();

        LOGGER.log(Level.INFO, "📄 Views encontradas: {0}", viewClassNames.size());

        for (String className : viewClassNames) {
            try {
                Class<?> clazz = Class.forName(className);
                registerView(clazz);
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "Classe de view não encontrada: {0}", className);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erro ao registar view: " + className, e);
            }
        }
    }

    /**
     * Registra uma view a partir da classe anotada.
     */
    private void registerView(Class<?> clazz) {
        try {
            ViewDescriptor descriptor = resourceAPI.registerFromAnnotatedClass(clazz);
            viewsFound++;
            LOGGER.log(Level.FINE, "✅ View registrada: {0} → {1}",
                    new Object[]{descriptor.getId(), descriptor.getFxmlUrl()});
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "⚠️ Falha ao registar view: " + clazz.getName() +
                    " - " + e.getMessage());
        }
    }

    // =============================================
    // SCAN DE IMAGENS (@RegisterImage)
    // =============================================

    /**
     * Escaneia e registra imagens anotadas com @RegisterImage / @RegisterImages.
     */
    private void scanImages(ScanResult result) {
        // Classes com @RegisterImages (container)
        Set<String> multiImageClassNames = result.getClassesWithAnnotation(RegisterImages.class).getNames();
        LOGGER.log(Level.INFO, "🖼️ Classes com @RegisterImages: {0}", multiImageClassNames.size());

        for (String className : multiImageClassNames) {
            try {
                Class<?> clazz = Class.forName(className);
                registerImagesFromClass(clazz);
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "Classe de imagens não encontrada: {0}", className);
            }
        }

        // Classes com @RegisterImage individual
        Set<String> singleImageClassNames = result.getClassesWithAnnotation(RegisterImage.class).getNames();
        LOGGER.log(Level.INFO, "🖼️ Classes com @RegisterImage: {0}", singleImageClassNames.size());

        for (String className : singleImageClassNames) {
            try {
                Class<?> clazz = Class.forName(className);
                registerSingleImage(clazz);
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "Classe de imagem não encontrada: {0}", className);
            }
        }
    }

    /**
     * Registra múltiplas imagens de uma classe (@RegisterImages).
     */
    private void registerImagesFromClass(Class<?> clazz) {
        try {
            List<ImageDescriptor> descriptors = resourceAPI.registerImagesFromClass(clazz);
            imagesFound += descriptors.size();
            LOGGER.log(Level.FINE, "✅ {0} imagens registradas da classe: {1}",
                    new Object[]{descriptors.size(), clazz.getSimpleName()});
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "⚠️ Falha ao registar imagens: " + clazz.getName() +
                    " - " + e.getMessage());
        }
    }

    /**
     * Registra uma imagem individual (@RegisterImage).
     */
    private void registerSingleImage(Class<?> clazz) {
        try {
            RegisterImage annotation = clazz.getAnnotation(RegisterImage.class);
            if (annotation != null) {
                ImageDescriptor descriptor = resourceAPI.registerImageFromAnnotation(annotation);
                imagesFound++;
                LOGGER.log(Level.FINE, "✅ Imagem registrada: {0} → {1}",
                        new Object[]{descriptor.getId(), descriptor.getImageUrl()});
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "⚠️ Falha ao registar imagem: " + clazz.getName() +
                    " - " + e.getMessage());
        }
    }

    // =============================================
    // ESTATÍSTICAS
    // =============================================

    private void printSummary(long durationMs) {
        LOGGER.log(Level.INFO, """

                ═══════════════════════════════════════
                 🏷️ SCAN DE RECURSOS CONCLUÍDO
                ═══════════════════════════════════════
                Duração: {0}ms
                📄 Views registradas:  {1}
                🖼️ Imagens registradas: {2}
                ═══════════════════════════════════════""",
                new Object[]{durationMs, viewsFound, imagesFound});
    }

    // ===== GETTERS =====

    public int getViewsFound() { return viewsFound; }
    public int getImagesFound() { return imagesFound; }
    public int getTotalResourcesFound() { return viewsFound + imagesFound; }
}