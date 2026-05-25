package com.ossobo.winterfx.di.scanner;

import com.ossobo.winterfx.di.annotations.RegisterImage;
import com.ossobo.winterfx.di.annotations.RegisterImages;
import com.ossobo.winterfx.di.annotations.RegisterView;
import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.registry.ResourceRegistry;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 🏷️ AnnotationScanner v3.1 - CORRIGIDO!
 *
 * <p>🔥 Correções:</p>
 * <ul>
 *   <li>✅ getAnnotationsByType() para @RegisterImage/@RegisterImages</li>
 *   <li>✅ resolveResource() usa sourceClass.getResource() PRIMEIRO</li>
 *   <li>✅ Tratamento correto de barra inicial no path</li>
 * </ul>
 */
public final class AnnotationScanner {

    private static final Logger LOGGER = Logger.getLogger(AnnotationScanner.class.getName());

    private final ResourceRegistry resourceRegistry;
    private final ClassLoader classLoader;
    private final String[] basePackages;

    private int viewsFound = 0;
    private int imagesFound = 0;

    public AnnotationScanner(ResourceRegistry resourceRegistry, ClassLoader classLoader, String... basePackages) {
        this.resourceRegistry = resourceRegistry;
        this.classLoader = classLoader;
        this.basePackages = (basePackages != null && basePackages.length > 0) ? basePackages : new String[]{""};
    }

    public AnnotationScanner(ResourceRegistry resourceRegistry, String... basePackages) {
        this(resourceRegistry, Thread.currentThread().getContextClassLoader(), basePackages);
    }

    public void scanAndRegister() {
        LOGGER.log(Level.INFO, "🏷️ Iniciando scan de recursos nos pacotes: {0}", String.join(", ", basePackages));
        long startTime = System.currentTimeMillis();
        try (ScanResult result = new ClassGraph().enableAnnotationInfo().enableClassInfo().acceptPackages(basePackages).scan()) {
            scanViews(result);
            scanImages(result);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Falha no scan: {0}", e.getMessage());
            throw new RuntimeException("Falha no escaneamento de recursos", e);
        }
        printSummary(System.currentTimeMillis() - startTime);
    }

    // =============================================
    // SCAN DE VIEWS
    // =============================================

    private void scanViews(ScanResult result) {
        List<String> names = result.getClassesWithAnnotation(RegisterView.class).getNames();
        LOGGER.log(Level.INFO, "📄 Views encontradas: {0}", names.size());
        for (String className : names) {
            try {
                Class<?> clazz = Class.forName(className);
                registerView(clazz);
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "⚠️ Classe não encontrada: {0}", className);
            }
        }
    }

    private void registerView(Class<?> clazz) {
        try {
            RegisterView ann = clazz.getAnnotation(RegisterView.class);
            if (ann == null) return;
            URL fxmlUrl = resolveResource(clazz, ann.fxml());
            if (fxmlUrl == null) {
                LOGGER.warning("⚠️ FXML não encontrado: " + ann.fxml());
                return;
            }
            ViewDescriptor d = ViewDescriptor.builder()
                    .id(ann.id()).fxmlUrl(fxmlUrl).controllerClass(clazz)
                    .title(ann.title()).width(ann.width()).height(ann.height())
                    .resizable(ann.resizable()).centered(ann.centered()).alwaysOnTop(ann.alwaysOnTop())
                    .viewType(ann.viewType()).cssMode(ann.cssMode()).modeUse(ann.modeUse())
                    .origin(ann.origin()).description(ann.description())
                    .tags(ann.tags().length > 0 ? List.of(ann.tags()) : null)
                    .encoding(ann.encoding()).initMethod(ann.initMethod())
                    .eager(ann.eager()).loadOrder(ann.loadOrder()).stageStyle(ann.stageStyle())
                    .build();
            resourceRegistry.register(d);
            viewsFound++;
        } catch (Exception e) {
            LOGGER.warning("⚠️ Falha ao registar view: " + clazz.getName() + " - " + e.getMessage());
        }
    }

    // =============================================
    // 🔥 SCAN DE IMAGENS (CORRIGIDO!)
    // =============================================

    private void scanImages(ScanResult result) {
        List<String> classNames = new ArrayList<>();
        classNames.addAll(result.getClassesWithAnnotation(RegisterImages.class).getNames());
        classNames.addAll(result.getClassesWithAnnotation(RegisterImage.class).getNames());
        LOGGER.log(Level.INFO, "🖼️ Classes com @RegisterImage: {0}", classNames.size());

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                for (RegisterImage ann : clazz.getAnnotationsByType(RegisterImage.class)) {
                    registerImageFromAnnotation(clazz, ann);
                }
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "⚠️ Classe não encontrada: {0}", className);
            }
        }
    }

    private void registerImageFromAnnotation(Class<?> sourceClass, RegisterImage ann) {
        URL imageUrl = resolveResource(sourceClass, ann.src());
        if (imageUrl == null) {
            LOGGER.warning("⚠️ Imagem não encontrada: " + ann.src());
            return;
        }
        ImageDescriptor d = ImageDescriptor.builder()
                .id(ann.id()).url(imageUrl).imageType(ann.imageType())
                .preferredWidth(ann.preferredWidth()).preferredHeight(ann.preferredHeight())
                .preserveRatio(ann.preserveRatio()).smooth(ann.smooth())
                .description(ann.description()).tags(ann.tags()).origin(ann.origin())
                .build();
        resourceRegistry.register(d);
        imagesFound++;
        LOGGER.log(Level.FINE, "✅ Imagem registrada: {0} → {1}", new Object[]{ann.id(), imageUrl});
    }

    // =============================================
    // 🔥 RESOLUÇÃO DE RECURSOS (CORRIGIDO!)
    // =============================================

    private URL resolveResource(Class<?> sourceClass, String path) {
        if (path == null || path.isBlank()) return null;
        if (path.startsWith("http:") || path.startsWith("https:") || path.startsWith("file:") || path.startsWith("jar:")) {
            try { return new URL(path); } catch (Exception e) { return null; }
        }
        // 1. ClassLoader da classe fonte
        if (sourceClass != null) {
            URL url = sourceClass.getResource(path.startsWith("/") ? path : "/" + path);
            if (url != null) return url;
        }
        // 2. ContextClassLoader da aplicação
        String clean = path.startsWith("/") ? path.substring(1) : path;
        URL url = classLoader.getResource(clean);
        if (url != null) return url;
        // 3. Fallback
        url = getClass().getClassLoader().getResource(clean);
        if (url != null) return url;
        LOGGER.warning("⚠️ Recurso não encontrado: " + path);
        return null;
    }

    private URL resolveResource(String path) { return resolveResource(null, path); }

    private void printSummary(long durationMs) {
        LOGGER.log(Level.INFO, "🏷️ Scan concluído em {0}ms - Views: {1}, Imagens: {2}", new Object[]{durationMs, viewsFound, imagesFound});
    }

    public int getViewsFound() { return viewsFound; }
    public int getImagesFound() { return imagesFound; }
    public int getTotalResourcesFound() { return viewsFound + imagesFound; }
}