package com.ossobo.winterfx.scanner;

import com.ossobo.winterfx.imagemanager.anotations.RegisterImage;
import com.ossobo.winterfx.imagemanager.anotations.RegisterImages;
import com.ossobo.winterfx.notifications.anotations.RegisterNotification;
import com.ossobo.winterfx.notifications.enums.NotificationType;
import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.AlertType;
import com.ossobo.winterfx.resources.enums.ResourceType;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.anotations.RegisterView;
import com.ossobo.winterfx.resources.enums.ModeUse;

import io.github.classgraph.ScanResult;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Scanner de anotações de recursos (Views, Imagens, Notificações).
 *
 * <p><b>Versão 2.0 - Extração completa de campos</b></p>
 *
 * @version 2.0
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

    // ============================================================
    // VIEWS (@RegisterView)
    // ============================================================

    private void scanViews(ResourceRegistry registry) {
        var classesWithAnnotation = scanResult.getClassesWithAnnotation(RegisterView.class);

        for (String className : classesWithAnnotation.getNames()) {
            Class<?> clazz = loadClass(className);
            if (clazz == null) {
                continue;
            }
            registerView(clazz, registry);
        }
    }

    private void registerView(Class<?> clazz, ResourceRegistry registry) {
        // ✅ Guardar ID para usar no catch
        String viewId = "unknown";

        try {
            RegisterView ann = clazz.getAnnotation(RegisterView.class);
            if (ann == null) return;

            viewId = ann.id();  // ← Guardar ID

            // ========== RESOLVER FXML ==========
            URL fxmlUrl = resolveResource(clazz, ann.fxml());
            if (fxmlUrl == null) {
                return;
            }

            // ========== RESOLVER CSS ==========
            URL primaryCssUrl = null;
            if (!ann.primaryCss().isEmpty()) {
                primaryCssUrl = resolveResource(clazz, ann.primaryCss());
            }

            List<URL> additionalCssUrls = new ArrayList<>();
            for (String cssPath : ann.additionalCss()) {
                if (!cssPath.isEmpty()) {
                    URL cssUrl = resolveResource(clazz, cssPath);
                    if (cssUrl != null) {
                        additionalCssUrls.add(cssUrl);
                    }
                }
            }

            // ========== RESOLVER ICON, SOUND, ALERT_ICON ==========
            URL iconUrl = null;
            if (!ann.icon().isEmpty()) {
                iconUrl = resolveResource(clazz, ann.icon());
            }

            URL soundUrl = null;
            if (!ann.sound().isEmpty()) {
                soundUrl = resolveResource(clazz, ann.sound());
            }

            URL alertIconUrl = null;
            if (!ann.alertIcon().isEmpty()) {
                alertIconUrl = resolveResource(clazz, ann.alertIcon());
            }

            // ========== RESOLVER RESOURCE BUNDLE ==========
            URL resourceBundleUrl = null;
            if (!ann.resourceBundle().isEmpty()) {
                resourceBundleUrl = resolveResource(clazz, ann.resourceBundle());
            }

            // ========== CONSTRUIR STYLE CLASSES ==========
            List<String> styleClasses = new ArrayList<>();
            if (ann.styleClasses().length > 0) {
                styleClasses.addAll(List.of(ann.styleClasses()));
            }

            styleClasses.add("view-" + ann.viewType().name().toLowerCase());
            if (ann.modeUse() == ModeUse.ALERT) {
                styleClasses.add("view-alert");
            }
            if (ann.centered()) {
                styleClasses.add("view-centered");
            }

            // ========== CONSTRUIR TAGS ==========
            List<String> tags = new ArrayList<>();
            if (ann.tags().length > 0) {
                tags.addAll(List.of(ann.tags()));
            }
            tags.add(ann.viewType().name().toLowerCase());
            tags.add(ann.modeUse().name().toLowerCase());

            // ========== CONSTRUIR ROLES ALLOWED ==========
            List<String> rolesAllowed = new ArrayList<>();
            if (ann.rolesAllowed().length > 0) {
                rolesAllowed.addAll(List.of(ann.rolesAllowed()));
            }

            // ========== CONSTRUIR PUBLISHES ==========
            List<String> publishes = new ArrayList<>();
            if (ann.publishes().length > 0) {
                publishes.addAll(List.of(ann.publishes()));
            }

            // ========== CONSTRUIR SUBSCRIBES ==========
            List<String> subscribes = new ArrayList<>();
            if (ann.subscribes().length > 0) {
                subscribes.addAll(List.of(ann.subscribes()));
            }

            // ========== CONSTRUIR DESCRIPTOR ==========
            ViewDescriptor descriptor = ViewDescriptor.builder()
                    .id(ann.id())
                    .fxmlUrl(fxmlUrl)
                    .controllerClass(clazz)
                    .title(ann.title())
                    .description(ann.description())
                    .tags(tags.isEmpty() ? null : tags)
                    .origin(ann.origin())
                    .encoding(ann.encoding())
                    .resourceBundle(resourceBundleUrl != null ? resourceBundleUrl.toString() : null)
                    .initMethod(ann.initMethod())
                    .managedController(ann.managedController())
                    .viewType(ann.viewType())
                    .eager(ann.eager())
                    .loadOrder(ann.loadOrder())
                    .cssMode(ann.cssMode())
                    .modeUse(ann.modeUse())
                    .width(ann.width())
                    .height(ann.height())
                    .resizable(ann.resizable())
                    .centered(ann.centered())
                    .alwaysOnTop(ann.alwaysOnTop())
                    .stageStyle(ann.stageStyle())
                    .primaryCss(primaryCssUrl)
                    .additionalCss(additionalCssUrls.isEmpty() ? null : additionalCssUrls)
                    .styleClasses(styleClasses.isEmpty() ? null : styleClasses)
                    .icon(ann.icon())
                    .iconUrl(iconUrl)
                    .sound(ann.sound())
                    .soundUrl(soundUrl)
                    .alertIcon(ann.alertIcon())
                    .alertIconUrl(alertIconUrl)
                    .alertType(ann.alertType())
                    .modality(ann.modality())
                    .confirmText(ann.confirmText())
                    .cancelText(ann.cancelText())
                    .confirmationRequired(ann.confirmationRequired())
                    .autoCloseMillis(ann.autoCloseMillis())
                    .rolesAllowed(rolesAllowed.isEmpty() ? null : rolesAllowed)
                    .authenticated(ann.authenticated())
                    .publishes(publishes.isEmpty() ? null : publishes)
                    .subscribes(subscribes.isEmpty() ? null : subscribes)
                    .build();

            registry.register(descriptor);
            viewsFound++;

        } catch (Exception e) {
        }
    }

    // ============================================================
    // IMAGENS (@RegisterImage)
    // ============================================================

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
        String imageId = ann != null ? ann.id() : "unknown";

        try {
            URL imageUrl = resolveResource(sourceClass, ann.src());
            if (imageUrl == null) {
                return;
            }

            ImageDescriptor descriptor = ImageDescriptor.builder()
                    .id(ann.id())
                    .url(imageUrl)
                    .src(ann.src())
                    .origin(ann.origin())
                    .imageType(ann.imageType())
                    .preferredWidth(ann.preferredWidth())
                    .preferredHeight(ann.preferredHeight())
                    .preserveRatio(ann.preserveRatio())
                    .smooth(ann.smooth())
                    .description(ann.description())
                    .tags(ann.tags())
                    .build();

            registry.register(descriptor);
            imagesFound++;

        } catch (Exception e) {
        }
    }

    // ============================================================
    // NOTIFICAÇÕES (@RegisterNotification)
    // ============================================================

    private void scanNotifications(ResourceRegistry registry) {
        var classesWithAnnotation = scanResult.getClassesWithAnnotation(RegisterNotification.class);

        for (String className : classesWithAnnotation.getNames()) {
            Class<?> clazz = loadClass(className);
            if (clazz == null) continue;

            RegisterNotification ann = clazz.getAnnotation(RegisterNotification.class);
            if (ann == null) continue;

            registerNotification(clazz, ann, registry);
        }
    }

    private void registerNotification(Class<?> clazz, RegisterNotification ann, ResourceRegistry registry) {
        String notificationId = ann != null ? ann.id() : "unknown";

        try {
            // ========== RESOLVER FXML ==========
            URL fxmlUrl = null;
            if (!ann.fxml().isEmpty()) {
                fxmlUrl = resolveResource(clazz, ann.fxml());
                if (fxmlUrl == null) {
                    fxmlUrl = resolveDefaultNotificationFxml(ann.id());
                }
            } else {
                fxmlUrl = resolveDefaultNotificationFxml(ann.id());
            }

            if (fxmlUrl == null) {
                return;
            }

            // ========== RESOLVER CSS ==========
            URL cssUrl = null;
            if (!ann.css().isEmpty()) {
                cssUrl = resolveResource(clazz, ann.css());
            }

            List<URL> additionalCssUrls = new ArrayList<>();
            for (String cssPath : ann.additionalCss()) {
                if (!cssPath.isEmpty()) {
                    URL url = resolveResource(clazz, cssPath);
                    if (url != null) {
                        additionalCssUrls.add(url);
                    }
                }
            }

            // ========== RESOLVER ICONS E SOUNDS ==========
            URL iconUrl = null;
            if (!ann.icon().isEmpty()) {
                iconUrl = resolveResource(clazz, ann.icon());
            }

            URL soundUrl = null;
            if (!ann.sound().isEmpty()) {
                soundUrl = resolveResource(clazz, ann.sound());
            }

            URL alertIconUrl = null;
            if (!ann.alertIcon().isEmpty()) {
                alertIconUrl = resolveResource(clazz, ann.alertIcon());
            }

            // ========== CONVERTER TIPO ==========
            AlertType alertType = convertToAlertType(ann.type());

            // ========== STYLE CLASSES ==========
            List<String> styleClasses = buildNotificationStyleClasses(ann);

            // ========== CONSTRUIR DESCRIPTOR ==========
            ViewDescriptor descriptor = ViewDescriptor.builder()
                    .id(ann.id())
                    .fxmlUrl(fxmlUrl)
                    .controllerClass(ann.controllerClass() != void.class ? ann.controllerClass() : clazz)
                    .title(!ann.title().isEmpty() ? ann.title() : ann.id())
                    .description(ann.description())
                    .tags(ann.tags().length > 0 ? List.of(ann.tags()) : null)
                    .origin(ann.origin())
                    .encoding("UTF-8")
                    .initMethod(ann.initMethod())
                    .managedController(ann.managedController())
                    .viewType(ann.viewType())
                    .eager(ann.eager())
                    .loadOrder(ann.loadOrder())
                    .cssMode(ann.cssMode())
                    .primaryCss(cssUrl)
                    .additionalCss(additionalCssUrls.isEmpty() ? null : additionalCssUrls)
                    .styleClasses(styleClasses.isEmpty() ? null : styleClasses)
                    .modeUse(ann.modeUse())
                    .width(ann.width() > 0 ? ann.width() : 350)
                    .height(ann.height() > 0 ? ann.height() : 120)
                    .resizable(ann.resizable())
                    .centered(ann.centered())
                    .alwaysOnTop(ann.alwaysOnTop())
                    .stageStyle(ann.stageStyle())
                    .iconUrl(iconUrl)
                    .icon(ann.icon())
                    .alertType(alertType)
                    .autoCloseMillis(ann.duration())
                    .soundUrl(soundUrl)
                    .sound(ann.sound())
                    .alertIconUrl(alertIconUrl)
                    .alertIcon(ann.alertIcon())
                    .confirmText(ann.confirmText())
                    .cancelText(ann.cancelText())
                    .confirmationRequired(ann.confirmationRequired())
                    .rolesAllowed(ann.rolesAllowed().length > 0 ? List.of(ann.rolesAllowed()) : null)
                    .authenticated(ann.authenticated())
                    .publishes(ann.publishes().length > 0 ? List.of(ann.publishes()) : null)
                    .subscribes(ann.subscribes().length > 0 ? List.of(ann.subscribes()) : null)
                    .resourceBundle(!ann.resourceBundle().isEmpty() ? ann.resourceBundle() : null)
                    .build();

            registry.register(descriptor);
            notificationsFound++;

        } catch (Exception e) {
        }
    }

    /**
     * Converte NotificationType para AlertType.
     */
    private AlertType convertToAlertType(NotificationType type) {
        if (type == null) return AlertType.INFO;
        return switch (type) {
            case INFO -> AlertType.INFO;
            case SUCCESS -> AlertType.SUCCESS;
            case WARNING -> AlertType.WARNING;
            case ERROR -> AlertType.ERROR;
            case CRITICAL -> AlertType.CRITICAL;
            case CONFIRMATION -> AlertType.CONFIRMATION;
            default -> AlertType.INFO;
        };
    }

    /**
     * Constrói lista de classes de estilo para a notificação.
     */
    private List<String> buildNotificationStyleClasses(RegisterNotification ann) {
        List<String> classes = new ArrayList<>();

        classes.add("notification-" + ann.type().name().toLowerCase());
        classes.add("notification-position-" + ann.position().name().toLowerCase());
        classes.add("notification-animation-" + ann.animation());

        if (ann.closable()) {
            classes.add("notification-closable");
        } else {
            classes.add("notification-non-closable");
        }

        if (ann.modal()) {
            classes.add("notification-modal");
        }

        if (ann.alwaysOnTop()) {
            classes.add("notification-top");
        }

        return classes;
    }

    /**
     * Resolve o FXML padrão para notificações.
     */
    private URL resolveDefaultNotificationFxml(String id) {
        String defaultPath = "/com/ossobo/winterfx/notifications/fxmls/" + id + ".fxml";
        URL url = classLoader.getResource(defaultPath.startsWith("/") ? defaultPath.substring(1) : defaultPath);

        if (url == null) {
            defaultPath = "/com/ossobo/winterfx/notifications/fxmls/default_notification.fxml";
            url = classLoader.getResource(defaultPath.startsWith("/") ? defaultPath.substring(1) : defaultPath);
        }

        return url;
    }

    // ============================================================
    // RESOLVER RECURSOS
    // ============================================================

    private URL resolveResource(Class<?> sourceClass, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        if (path.startsWith("http:") || path.startsWith("https:")
                || path.startsWith("file:") || path.startsWith("jar:")) {
            try {
                return URI.create(path).toURL();
            } catch (Exception e) {
                return null;
            }
        }

        String clean = path.startsWith("/") ? path.substring(1) : path;

        if (sourceClass != null) {
            ClassLoader sourceLoader = sourceClass.getClassLoader();
            if (sourceLoader != null) {
                URL url = sourceLoader.getResource(clean);
                if (url != null) {
                    return url;
                }
            }
        }

        URL url = classLoader.getResource(clean);
        if (url != null) {
            return url;
        }

        ClassLoader frameworkClassLoader = getClass().getClassLoader();
        if (frameworkClassLoader != null && frameworkClassLoader != classLoader) {
            url = frameworkClassLoader.getResource(clean);
            if (url != null) {
                return url;
            }
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

    // ============================================================
    // GETTERS
    // ============================================================

    public int getViewsFound() { return viewsFound; }
    public int getImagesFound() { return imagesFound; }
    public int getNotificationsFound() { return notificationsFound; }
}