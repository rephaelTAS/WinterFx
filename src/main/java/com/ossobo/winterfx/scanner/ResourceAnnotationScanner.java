package com.ossobo.winterfx.scanner;

import com.ossobo.winterfx.imagemanager.anotations.RegisterImage;
import com.ossobo.winterfx.imagemanager.anotations.RegisterImages;
import com.ossobo.winterfx.notifications.anotations.RegisterNotification;
import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.anotations.RegisterView;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 📋 ResourceAnnotationScanner v1.1
 *
 * Responsabilidade: Encontrar e registrar RECURSOS.
 * Busca por: @RegisterView, @RegisterImage, @RegisterNotification
 *
 * Usa ClassGraph DIRETO — não depende do DiContainer.
 *
 * <p><b>🔥 CORREÇÃO v1.1:</b> resolveResource() refatorado.
 * Agora usa ContextClassLoader PRIMEIRO (sem barra no início),
 * alinhado com ViewAnnotationResolver e ImageAnnotationResolver.</p>
 */
public final class ResourceAnnotationScanner {

    private static final Logger LOGGER = Logger.getLogger(ResourceAnnotationScanner.class.getName());

    private final ClassLoader contextClassLoader;
    private final String[] basePackages;

    private int viewsFound = 0;
    private int imagesFound = 0;
    private int notificationsFound = 0;

    public ResourceAnnotationScanner(String... basePackages) {
        this.basePackages = (basePackages != null && basePackages.length > 0)
                ? basePackages : new String[]{""};
        this.contextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    /**
     * Executa scan + registro de recursos.
     */
    public int scanAndRegister(ResourceRegistry registry) {
        LOGGER.log(Level.INFO, "📋 Iniciando scan de recursos nos pacotes: {0}",
                String.join(", ", basePackages));
        long startTime = System.currentTimeMillis();

        try (ScanResult result = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .acceptPackages(basePackages)
                .scan()) {

            scanViews(result, registry);
            scanImages(result, registry);
            scanNotifications(result, registry);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Falha no scan de recursos: {0}", e.getMessage());
            throw new RuntimeException("Falha no escaneamento de recursos", e);
        }

        int total = viewsFound + imagesFound + notificationsFound;
        long duration = System.currentTimeMillis() - startTime;
        LOGGER.log(Level.INFO, "✅ Recursos registrados: {0} ({1}ms) - Views: {2}, Imagens: {3}, Notificações: {4}",
                new Object[]{total, duration, viewsFound, imagesFound, notificationsFound});

        return total;
    }

    // =============================================
    // SCAN DE VIEWS
    // =============================================

    private void scanViews(ScanResult result, ResourceRegistry registry) {
        List<String> names = result.getClassesWithAnnotation(RegisterView.class).getNames();
        LOGGER.log(Level.INFO, "📄 Views encontradas: {0}", names.size());

        for (String className : names) {
            try {
                Class<?> clazz = Class.forName(className);
                registerView(clazz, registry);
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "⚠️ Classe não encontrada: {0}", className);
            }
        }
    }

    private void registerView(Class<?> clazz, ResourceRegistry registry) {
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
            registry.register(d);
            viewsFound++;
            LOGGER.log(Level.FINE, "✅ View registrada: {0}", ann.id());
        } catch (Exception e) {
            LOGGER.warning("⚠️ Falha ao registar view: " + clazz.getName() + " - " + e.getMessage());
        }
    }

    // =============================================
    // SCAN DE IMAGENS
    // =============================================

    private void scanImages(ScanResult result, ResourceRegistry registry) {
        List<String> classNames = new ArrayList<>();
        classNames.addAll(result.getClassesWithAnnotation(RegisterImages.class).getNames());
        classNames.addAll(result.getClassesWithAnnotation(RegisterImage.class).getNames());
        LOGGER.log(Level.INFO, "🖼️ Classes com @RegisterImage: {0}", classNames.size());

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                for (RegisterImage ann : clazz.getAnnotationsByType(RegisterImage.class)) {
                    registerImage(clazz, ann, registry);
                }
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "⚠️ Classe não encontrada: {0}", className);
            }
        }
    }

    private void registerImage(Class<?> sourceClass, RegisterImage ann, ResourceRegistry registry) {
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
        registry.register(d);
        imagesFound++;
        LOGGER.log(Level.FINE, "✅ Imagem registrada: {0}", ann.id());
    }

    // =============================================
    // SCAN DE NOTIFICAÇÕES
    // =============================================

    private void scanNotifications(ScanResult result, ResourceRegistry registry) {
        List<String> names = result.getClassesWithAnnotation(RegisterNotification.class).getNames();
        LOGGER.log(Level.INFO, "🔔 Notificações encontradas: {0}", names.size());

        for (String className : names) {
            try {
                Class<?> clazz = Class.forName(className);
                RegisterNotification ann = clazz.getAnnotation(RegisterNotification.class);
                if (ann != null) {
                    URL fxmlUrl = resolveResource(clazz, ann.fxml());
                    ViewDescriptor d = ViewDescriptor.builder()
                            .id(ann.id()).fxmlUrl(fxmlUrl)
                            .title(ann.id())
                            .origin(com.ossobo.winterfx.resources.enums.ResourceOrigin.APPLICATION)
                            .build();
                    registry.register(d);
                    notificationsFound++;
                }
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "⚠️ Classe não encontrada: {0}", className);
            }
        }
    }

    // =============================================
    // RESOLUÇÃO DE URLs — 🔥 CORRIGIDO v1.1
    // =============================================

    /**
     * 🔥 Resolve um caminho de recurso para URL.
     *
     * <p><b>Ordem de resolução (CORRIGIDA):</b></p>
     * <ol>
     *   <li><b>ContextClassLoader</b> (aplicação cliente) — 👈 PRIMEIRO!
     *       <br>Usa {@code getResource(clean)} SEM barra no início.
     *       Este é o ClassLoader que "vê" os recursos do projeto.</li>
     *   <li><b>ClassLoader da classe fonte</b> — classe que tem a anotação
     *       <br>Usa {@code getClassLoader().getResource(clean)} SEM barra.</li>
     *   <li><b>ClassLoader do WinterFX</b> (fallback)
     *       <br>Para recursos internos do framework.</li>
     *   <li><b>ClassGraph</b> (último recurso)
     *       <br>Consegue acessar recursos em qualquer módulo/JAR.</li>
     * </ol>
     *
     * <p><b>Por que a ordem antiga falhava?</b>
     * {@code sourceClass.getResource("/" + path)} adiciona barra no início,
     * e {@code ClassLoader.getResource()} NÃO aceita caminhos com "/" inicial
     * em ambientes modulares (JPMS).</p>
     *
     * @param sourceClass classe que contém a anotação (pode ser null)
     * @param path        caminho do recurso (ex: "/com/biblioteca/images/logo.png")
     * @return URL do recurso ou null se não encontrado
     */
    private URL resolveResource(Class<?> sourceClass, String path) {
        if (path == null || path.isBlank()) return null;

        // URLs absolutas — retorna direto
        if (path.startsWith("http:") || path.startsWith("https:")
                || path.startsWith("file:") || path.startsWith("jar:")) {
            try { return new URL(path); } catch (Exception e) { return null; }
        }

        // 🔧 Remove barra inicial — ClassLoader.getResource() NÃO aceita "/" no início
        String clean = path.startsWith("/") ? path.substring(1) : path;

        // ═══════════════════════════════════════════════
        // 1. ContextClassLoader da aplicação cliente
        //    Mesmo padrão do ViewAnnotationResolver e ImageAnnotationResolver
        // ═══════════════════════════════════════════════
        URL url = contextClassLoader.getResource(clean);
        if (url != null) {
            LOGGER.log(Level.FINE, "✅ Resolvido via ContextClassLoader: {0}", clean);
            return url;
        }

        // ═══════════════════════════════════════════════
        // 2. ClassLoader da classe fonte (classe anotada)
        // ═══════════════════════════════════════════════
        if (sourceClass != null) {
            ClassLoader sourceLoader = sourceClass.getClassLoader();
            if (sourceLoader != null && sourceLoader != contextClassLoader) {
                url = sourceLoader.getResource(clean);
                if (url != null) {
                    LOGGER.log(Level.FINE, "✅ Resolvido via SourceClassLoader: {0}", clean);
                    return url;
                }
            }
        }

        // ═══════════════════════════════════════════════
        // 3. Fallback: ClassLoader do WinterFX
        // ═══════════════════════════════════════════════
        ClassLoader winterClassLoader = getClass().getClassLoader();
        if (winterClassLoader != contextClassLoader) {
            url = winterClassLoader.getResource(clean);
            if (url != null) {
                LOGGER.log(Level.FINE, "✅ Resolvido via WinterFX ClassLoader: {0}", clean);
                return url;
            }
        }

        // ═══════════════════════════════════════════════
// 4. 🆕 ClassGraph — último recurso
//    Consegue acessar recursos em qualquer módulo/JAR/diretório
// ═══════════════════════════════════════════════
        try {
            int lastSlash = clean.lastIndexOf('/');
            String searchPath = lastSlash > 0 ? clean.substring(0, lastSlash + 1) : "";

            try (ScanResult scan = new ClassGraph()
                    .acceptPaths(searchPath)
                    .scan()) {
                // 🔧 CORRIGIDO: getAllResources().get() retorna ResourceList,
                //    precisamos iterar para obter as URLs
                io.github.classgraph.ResourceList resources = scan.getAllResources().get(clean);
                if (resources != null && !resources.isEmpty()) {
                    url = resources.get(0).getURL();  // 👈 Pega o primeiro recurso
                    if (url != null) {
                        LOGGER.log(Level.FINE, "✅ Resolvido via ClassGraph: {0}", clean);
                        return url;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINEST, "ClassGraph fallback falhou: {0}", e.getMessage());
        }

        LOGGER.warning("⚠️ Recurso não encontrado: " + path);
        return null;
    }

    // =============================================
    // GETTERS
    // =============================================

    public int getViewsFound() { return viewsFound; }
    public int getImagesFound() { return imagesFound; }
    public int getNotificationsFound() { return notificationsFound; }
    public int getTotalResourcesFound() { return viewsFound + imagesFound + notificationsFound; }
}