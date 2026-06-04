package com.ossobo.winterfx.scanner;

import com.ossobo.winterfx.imagemanager.anotations.RegisterImage;
import com.ossobo.winterfx.imagemanager.anotations.RegisterImages;
import com.ossobo.winterfx.notifications.anotations.RegisterNotification;
import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.anotations.RegisterView;

import io.github.classgraph.ScanResult;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Scanner de recursos do WinterFX.
 *
 * <p>Recebe um ScanResult compartilhado do ScannerEngine.</p>
 */
public final class ResourceAnnotationScanner {

    private final ScanResult scanResult;
    private final ClassLoader classLoader;

    private int viewsFound = 0;
    private int imagesFound = 0;
    private int notificationsFound = 0;

    public ResourceAnnotationScanner(ScanResult scanResult) {
        this.scanResult = scanResult;
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    public int scanAndRegister(ResourceRegistry registry) {
        viewsFound = 0;
        imagesFound = 0;
        notificationsFound = 0;

        scanViews(registry);
        scanImages(registry);
        scanNotifications(registry);

        return viewsFound + imagesFound + notificationsFound;
    }

    private void scanViews(ResourceRegistry registry) {
        for (String className : scanResult.getClassesWithAnnotation(RegisterView.class).getNames()) {
            Class<?> clazz = loadClass(className);
            if (clazz == null) continue;

            registerView(clazz, registry);
        }
    }

    private void registerView(Class<?> clazz, ResourceRegistry registry) {
        try {
            RegisterView ann = clazz.getAnnotation(RegisterView.class);
            if (ann == null) return;

            URL fxmlUrl = resolveResource(clazz, ann.fxml());
            if (fxmlUrl == null) return;

            ViewDescriptor descriptor = ViewDescriptor.builder()
                    .id(ann.id()).fxmlUrl(fxmlUrl).controllerClass(clazz)
                    .title(ann.title()).width(ann.width()).height(ann.height())
                    .resizable(ann.resizable()).centered(ann.centered()).alwaysOnTop(ann.alwaysOnTop())
                    .viewType(ann.viewType()).cssMode(ann.cssMode()).modeUse(ann.modeUse())
                    .origin(ann.origin()).description(ann.description())
                    .tags(ann.tags().length > 0 ? List.of(ann.tags()) : null)
                    .encoding(ann.encoding()).initMethod(ann.initMethod())
                    .eager(ann.eager()).loadOrder(ann.loadOrder()).stageStyle(ann.stageStyle())
                    .build();

            registry.register(descriptor);
            viewsFound++;
        } catch (Exception ignored) {}
    }

    private void scanImages(ResourceRegistry registry) {
        List<String> classNames = new ArrayList<>();
        classNames.addAll(scanResult.getClassesWithAnnotation(RegisterImages.class).getNames());
        classNames.addAll(scanResult.getClassesWithAnnotation(RegisterImage.class).getNames());

        for (String className : classNames) {
            Class<?> clazz = loadClass(className);
            if (clazz == null) continue;

            for (RegisterImage ann : clazz.getAnnotationsByType(RegisterImage.class)) {
                registerImage(clazz, ann, registry);
            }
        }
    }

    private void registerImage(Class<?> sourceClass, RegisterImage ann, ResourceRegistry registry) {
        URL imageUrl = resolveResource(sourceClass, ann.src());
        if (imageUrl == null) return;

        ImageDescriptor descriptor = ImageDescriptor.builder()
                .id(ann.id()).url(imageUrl).imageType(ann.imageType())
                .preferredWidth(ann.preferredWidth()).preferredHeight(ann.preferredHeight())
                .preserveRatio(ann.preserveRatio()).smooth(ann.smooth())
                .description(ann.description()).tags(ann.tags()).origin(ann.origin())
                .build();

        registry.register(descriptor);
        imagesFound++;
    }

    private void scanNotifications(ResourceRegistry registry) {
        for (String className : scanResult.getClassesWithAnnotation(RegisterNotification.class).getNames()) {
            Class<?> clazz = loadClass(className);
            if (clazz == null) continue;

            RegisterNotification ann = clazz.getAnnotation(RegisterNotification.class);
            if (ann == null) return;

            URL fxmlUrl = resolveResource(clazz, ann.fxml());
            ViewDescriptor descriptor = ViewDescriptor.builder()
                    .id(ann.id()).fxmlUrl(fxmlUrl).title(ann.id())
                    .origin(ResourceOrigin.APPLICATION).build();

            registry.register(descriptor);
            notificationsFound++;
        }
    }

    private URL resolveResource(Class<?> sourceClass, String path) {
        if (path == null || path.isBlank()) return null;

        if (path.startsWith("http:") || path.startsWith("https:")
                || path.startsWith("file:") || path.startsWith("jar:")) {
            try { return URI.create(path).toURL(); } catch (Exception e) { return null; }
        }

        String clean = path.startsWith("/") ? path.substring(1) : path;

        URL url = classLoader.getResource(clean);
        if (url != null) return url;

        if (sourceClass != null) {
            ClassLoader sourceLoader = sourceClass.getClassLoader();
            if (sourceLoader != null && sourceLoader != classLoader) {
                url = sourceLoader.getResource(clean);
                if (url != null) return url;
            }
        }

        ClassLoader frameworkClassLoader = getClass().getClassLoader();
        if (frameworkClassLoader != null && frameworkClassLoader != classLoader) {
            url = frameworkClassLoader.getResource(clean);
            if (url != null) return url;
        }

        return null;
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (Throwable e) {
            return null;
        }
    }

    public int getViewsFound() { return viewsFound; }
    public int getImagesFound() { return imagesFound; }
    public int getNotificationsFound() { return notificationsFound; }
}