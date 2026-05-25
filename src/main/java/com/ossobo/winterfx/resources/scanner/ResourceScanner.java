package com.ossobo.winterfx.resources.scanner;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.di.annotations.*;
import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.registry.ResourceRegistry;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResourceScanner {

    private static final Logger LOGGER = Logger.getLogger(ResourceScanner.class.getName());

    private final DiContainer diContainer;
    private final ResourceRegistry resourceRegistry;

    public ResourceScanner(DiContainer diContainer, ResourceRegistry resourceRegistry) {
        this.diContainer = Objects.requireNonNull(diContainer, "DiContainer não pode ser nulo");
        this.resourceRegistry = Objects.requireNonNull(resourceRegistry, "ResourceRegistry não pode ser nulo");
    }

    public ScanResult scanAll() {
        LOGGER.info("🔍 Iniciando scan de recursos...");
        int viewsFound = scanViews();
        int imagesFound = scanImages();
        int notificationsFound = scanNotifications();
        ScanResult result = new ScanResult(viewsFound, imagesFound, notificationsFound);
        LOGGER.log(Level.INFO, "✅ Scan concluído: {0}", result);
        return result;
    }

    public int scanViews() {
        Set<Class<?>> annotatedClasses = diContainer.findClassesWithAnnotation(RegisterView.class);
        int count = 0;
        for (Class<?> clazz : annotatedClasses) {
            try {
                RegisterView ann = clazz.getAnnotation(RegisterView.class);
                ViewDescriptor d = createViewDescriptor(clazz, ann);
                resourceRegistry.register(d);
                count++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "❌ Erro view: " + clazz.getName(), e);
            }
        }
        LOGGER.log(Level.INFO, "📄 Views: {0}", count);
        return count;
    }

    public int scanImages() {
        int count = 0;
        Set<Class<?>> imageClasses = diContainer.findClassesWithAnnotation(RegisterImage.class);
        for (Class<?> clazz : imageClasses) {
            try {
                RegisterImage ann = clazz.getAnnotation(RegisterImage.class);
                ImageDescriptor d = createImageDescriptor(ann, clazz);
                if (d != null) { resourceRegistry.register(d); count++; }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "❌ Erro imagem: " + clazz.getName(), e);
            }
        }
        Set<Class<?>> multiImageClasses = diContainer.findClassesWithAnnotation(RegisterImages.class);
        for (Class<?> clazz : multiImageClasses) {
            try {
                RegisterImages container = clazz.getAnnotation(RegisterImages.class);
                for (RegisterImage ann : container.value()) {
                    ImageDescriptor d = createImageDescriptor(ann, clazz);
                    if (d != null) { resourceRegistry.register(d); count++; }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "❌ Erro imagens: " + clazz.getName(), e);
            }
        }
        LOGGER.log(Level.INFO, "🖼️ Imagens: {0}", count);
        return count;
    }

    public int scanNotifications() {
        Set<Class<?>> classes = diContainer.findClassesWithAnnotation(RegisterNotification.class);
        int count = 0;
        for (Class<?> clazz : classes) {
            try {
                RegisterNotification ann = clazz.getAnnotation(RegisterNotification.class);
                ViewDescriptor d = createNotificationDescriptor(ann);
                resourceRegistry.register(d);
                count++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "❌ Erro notif: " + clazz.getName(), e);
            }
        }
        LOGGER.log(Level.INFO, "🔔 Notificações: {0}", count);
        return count;
    }

    public Map<Class<?>, List<Method>> findAnnotatedMethods(Class<? extends java.lang.annotation.Annotation> ac) {
        Set<Method> methods = diContainer.findMethodsWithAnnotation(ac);
        Map<Class<?>, List<Method>> result = new HashMap<>();
        for (Method m : methods) {
            result.computeIfAbsent(m.getDeclaringClass(), k -> new ArrayList<>()).add(m);
        }
        return result;
    }

    // ==================== CRIAÇÃO DE DESCRITORES ====================

    private ViewDescriptor createViewDescriptor(Class<?> clazz, RegisterView ann) {
        URL fxmlUrl = resolveFxmlUrl(clazz, ann.fxml());
        return ViewDescriptor.builder()
                .id(ann.id()).fxmlUrl(fxmlUrl).controllerClass(clazz)
                .title(ann.title()).width(ann.width()).height(ann.height())
                .origin(ResourceOrigin.APPLICATION).build();
    }

    // 🔥 CORRIGIDO: recebe a classe para usar o ClassLoader correto!
    private ImageDescriptor createImageDescriptor(RegisterImage ann, Class<?> sourceClass) {
        URL imageUrl = resolveImageUrl(ann.src(), sourceClass);
        if (imageUrl == null) {
            LOGGER.warning("⚠️ Imagem ignorada: " + ann.src());
            return null;
        }
        return ImageDescriptor.builder()
                .id(ann.id()).url(imageUrl).origin(ResourceOrigin.APPLICATION).build();
    }

    // fallback sem sourceClass
    private ImageDescriptor createImageDescriptor(RegisterImage ann) {
        return createImageDescriptor(ann, null);
    }

    private ViewDescriptor createNotificationDescriptor(RegisterNotification ann) {
        URL fxmlUrl = resolveFxmlUrl(null, ann.fxml());
        return ViewDescriptor.builder()
                .id(ann.id()).fxmlUrl(fxmlUrl).title(ann.id())
                .origin(ResourceOrigin.APPLICATION).build();
    }

    // ==================== RESOLUÇÃO DE URLs ====================

    private URL resolveFxmlUrl(Class<?> clazz, String fxmlPath) {
        if (clazz != null) {
            URL url = clazz.getResource(fxmlPath);
            if (url != null) return url;
        }
        String clean = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath;
        URL url = getClass().getClassLoader().getResource(clean);
        if (url != null) return url;
        url = getClass().getClassLoader().getResource("/" + clean);
        if (url != null) return url;
        url = Thread.currentThread().getContextClassLoader().getResource(clean);
        if (url != null) return url;
        url = Thread.currentThread().getContextClassLoader().getResource("/" + clean);
        if (url != null) return url;
        LOGGER.warning("⚠️ FXML não encontrado: " + fxmlPath);
        return null;
    }

    // 🔥 CORRIGIDO: usa o ClassLoader da classe que registrou a imagem!
    private URL resolveImageUrl(String imagePath, Class<?> sourceClass) {
        String clean = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;

        // 1. ClassLoader da classe que tem @RegisterImage
        if (sourceClass != null) {
            URL url = sourceClass.getResource("/" + clean);
            if (url != null) { LOGGER.fine("✅ Imagem (sourceClass): " + clean); return url; }
            url = sourceClass.getResource(clean);
            if (url != null) { LOGGER.fine("✅ Imagem (sourceClass no /): " + clean); return url; }
        }

        // 2. ContextClassLoader da aplicação
        URL url = Thread.currentThread().getContextClassLoader().getResource(clean);
        if (url != null) { LOGGER.fine("✅ Imagem (ContextCL): " + clean); return url; }
        url = Thread.currentThread().getContextClassLoader().getResource("/" + clean);
        if (url != null) { LOGGER.fine("✅ Imagem (ContextCL /): " + clean); return url; }

        // 3. WinterFX ClassLoader
        url = getClass().getClassLoader().getResource(clean);
        if (url != null) return url;
        url = getClass().getClassLoader().getResource("/" + clean);
        if (url != null) return url;

        LOGGER.warning("⚠️ Imagem não encontrada: " + imagePath + " [sourceClass=" + (sourceClass != null ? sourceClass.getSimpleName() : "null") + "]");
        return null;
    }

    // fallback
    private URL resolveImageUrl(String imagePath) {
        return resolveImageUrl(imagePath, null);
    }

    // ==================== ScanResult ====================

    public static class ScanResult {
        private final int views, images, notifications;
        public ScanResult(int v, int i, int n) { views = v; images = i; notifications = n; }
        public int getViews() { return views; }
        public int getImages() { return images; }
        public int getNotifications() { return notifications; }
        public int getTotal() { return views + images + notifications; }
        @Override public String toString() {
            return String.format("ScanResult[views=%d, images=%d, notifications=%d, total=%d]", views, images, notifications, getTotal());
        }
    }
}