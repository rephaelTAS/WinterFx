package com.ossobo.winterfx.resources.descriptor;

import com.ossobo.winterfx.resources.enums.*;

import java.net.URL;
import java.util.List;
import java.util.Objects;

/**
 * ViewDescriptor v5.0 - Record puro
 *
 * Descreve uma view FXML ou alerta com todos os metadados.
 * Record de 47 componentes com validações centralizadas.
 */
public record ViewDescriptor(
        String id,
        URL fxmlUrl,
        ResourceOrigin origin,
        ViewType viewType,
        Class<?> controllerClass,
        boolean managedController,
        CssMode cssMode,
        URL primaryCss,
        List<URL> additionalCss,
        ModeUse modeUse,
        String title,
        int width,
        int height,
        boolean resizable,
        boolean centered,
        boolean alwaysOnTop,
        boolean maximized,
        boolean fullScreen,
        double minWidth,
        double minHeight,
        double maxWidth,
        double maxHeight,
        double opacity,
        StageStyle stageStyle,
        URL iconUrl,
        String icon,
        String sound,
        String alertIcon,
        boolean eager,
        int loadOrder,
        boolean closeOnExit,
        String closeConfirmation,
        String initMethod,
        String resourceBundle,
        List<String> styleClasses,
        List<String> tags,
        String description,
        String encoding,
        List<String> rolesAllowed,
        boolean authenticated,
        List<String> publishes,
        List<String> subscribes,
        AlertType alertType,
        Modality modality,
        URL soundUrl,
        URL alertIconUrl,
        boolean confirmationRequired,
        long autoCloseMillis,
        String confirmText,
        String cancelText
) implements ResourceDescriptor {

    /**
     * Construtor compacto: Validações e Transformações de Imutabilidade.
     */
    public ViewDescriptor {
        Objects.requireNonNull(id, "id é obrigatório");
        Objects.requireNonNull(fxmlUrl, "fxmlUrl é obrigatório");
        Objects.requireNonNull(modeUse, "modeUse é obrigatório");

        // Defaults e Imutabilidade Profunda
        origin = Objects.requireNonNullElse(origin, ResourceOrigin.APPLICATION);
        viewType = Objects.requireNonNullElse(viewType, ViewType.STATIC);
        cssMode = Objects.requireNonNullElse(cssMode, CssMode.NONE);
        stageStyle = Objects.requireNonNullElse(stageStyle, StageStyle.DECORATED);

        title = title != null ? title : "";
        width = width > 0 ? width : 800;
        height = height > 0 ? height : 600;
        opacity = opacity > 0 ? opacity : 1.0;
        initMethod = initMethod != null ? initMethod : "initialize";
        encoding = encoding != null ? encoding : "UTF-8";
        description = description != null ? description : "";
        confirmText = confirmText != null ? confirmText : "OK";
        cancelText = cancelText != null ? cancelText : "Cancelar";

        // List.copyOf garante imutabilidade
        additionalCss = additionalCss != null ? List.copyOf(additionalCss) : List.of();
        styleClasses = styleClasses != null ? List.copyOf(styleClasses) : List.of();
        tags = tags != null ? List.copyOf(tags) : List.of();
        rolesAllowed = rolesAllowed != null ? List.copyOf(rolesAllowed) : List.of();
        publishes = publishes != null ? List.copyOf(publishes) : List.of();
        subscribes = subscribes != null ? List.copyOf(subscribes) : List.of();

        if (modeUse == ModeUse.ALERT) {
            Objects.requireNonNull(alertType, "alertType é obrigatório para ALERT");
            Objects.requireNonNull(modality, "modality é obrigatório para ALERT");
        }
    }

    // ===== Implementação da Interface (Adaptação de nomes) =====
    @Override
    public URL url() { return fxmlUrl; }

    @Override
    public ResourceType resourceType() {
        return modeUse == ModeUse.ALERT ? ResourceType.ALERT : ResourceType.FXML;
    }

    // ===== Métodos de Negócio =====
    public URL getFxmlUrl() { return fxmlUrl; } // Alias legado
    public boolean isAlert() { return modeUse == ModeUse.ALERT; }
    public boolean isView() { return modeUse == ModeUse.VIEW; }

    // ===== Builder =====
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String id;
        private URL fxmlUrl;
        private ResourceOrigin origin = ResourceOrigin.APPLICATION;
        private ViewType viewType = ViewType.STATIC;
        private Class<?> controllerClass;
        private boolean managedController;
        private CssMode cssMode = CssMode.NONE;
        private URL primaryCss;
        private List<URL> additionalCss;
        private ModeUse modeUse = ModeUse.VIEW;
        private String title;
        private int width = 800;
        private int height = 600;
        private boolean resizable = true;
        private boolean centered = true;
        private boolean alwaysOnTop;
        private boolean maximized;
        private boolean fullScreen;
        private double minWidth = -1;
        private double minHeight = -1;
        private double maxWidth = -1;
        private double maxHeight = -1;
        private double opacity = 1.0;
        private StageStyle stageStyle = StageStyle.DECORATED;
        private URL iconUrl;
        private String icon;
        private String sound;
        private String alertIcon;
        private boolean eager;
        private int loadOrder;
        private boolean closeOnExit;
        private String closeConfirmation;
        private String initMethod = "initialize";
        private String resourceBundle;
        private List<String> styleClasses;
        private List<String> tags;
        private String description;
        private String encoding = "UTF-8";
        private List<String> rolesAllowed;
        private boolean authenticated;
        private List<String> publishes;
        private List<String> subscribes;
        private AlertType alertType;
        private Modality modality;
        private URL soundUrl;
        private URL alertIconUrl;
        private boolean confirmationRequired;
        private long autoCloseMillis;
        private String confirmText = "OK";
        private String cancelText = "Cancelar";

        public Builder id(String id) { this.id = id; return this; }
        public Builder fxmlUrl(URL url) { this.fxmlUrl = url; return this; }
        public Builder origin(ResourceOrigin origin) { this.origin = origin; return this; }
        public Builder viewType(ViewType vt) { this.viewType = vt; return this; }
        public Builder controllerClass(Class<?> cc) { this.controllerClass = cc; return this; }
        public Builder managedController(boolean mc) { this.managedController = mc; return this; }
        public Builder cssMode(CssMode cm) { this.cssMode = cm; return this; }
        public Builder primaryCss(URL css) { this.primaryCss = css; return this; }
        public Builder additionalCss(List<URL> css) { this.additionalCss = css; return this; }
        public Builder modeUse(ModeUse mu) { this.modeUse = mu; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder width(int width) { this.width = width; return this; }
        public Builder height(int height) { this.height = height; return this; }
        public Builder resizable(boolean r) { this.resizable = r; return this; }
        public Builder centered(boolean c) { this.centered = c; return this; }
        public Builder alwaysOnTop(boolean aot) { this.alwaysOnTop = aot; return this; }
        public Builder maximized(boolean m) { this.maximized = m; return this; }
        public Builder fullScreen(boolean fs) { this.fullScreen = fs; return this; }
        public Builder minWidth(double mw) { this.minWidth = mw; return this; }
        public Builder minHeight(double mh) { this.minHeight = mh; return this; }
        public Builder maxWidth(double mw) { this.maxWidth = mw; return this; }
        public Builder maxHeight(double mh) { this.maxHeight = mh; return this; }
        public Builder opacity(double o) { this.opacity = o; return this; }
        public Builder stageStyle(StageStyle ss) { this.stageStyle = ss; return this; }
        public Builder iconUrl(URL url) { this.iconUrl = url; return this; }
        public Builder icon(String icon) { this.icon = icon; return this; }
        public Builder sound(String sound) { this.sound = sound; return this; }
        public Builder alertIcon(String ai) { this.alertIcon = ai; return this; }
        public Builder eager(boolean e) { this.eager = e; return this; }
        public Builder loadOrder(int lo) { this.loadOrder = lo; return this; }
        public Builder closeOnExit(boolean coe) { this.closeOnExit = coe; return this; }
        public Builder closeConfirmation(String cc) { this.closeConfirmation = cc; return this; }
        public Builder initMethod(String im) { this.initMethod = im; return this; }
        public Builder resourceBundle(String rb) { this.resourceBundle = rb; return this; }
        public Builder styleClasses(List<String> sc) { this.styleClasses = sc; return this; }
        public Builder tags(List<String> t) { this.tags = t; return this; }
        public Builder description(String d) { this.description = d; return this; }
        public Builder encoding(String e) { this.encoding = e; return this; }
        public Builder rolesAllowed(List<String> ra) { this.rolesAllowed = ra; return this; }
        public Builder authenticated(boolean a) { this.authenticated = a; return this; }
        public Builder publishes(List<String> p) { this.publishes = p; return this; }
        public Builder subscribes(List<String> s) { this.subscribes = s; return this; }
        public Builder alertType(AlertType at) { this.alertType = at; return this; }
        public Builder modality(Modality m) { this.modality = m; return this; }
        public Builder soundUrl(URL url) { this.soundUrl = url; return this; }
        public Builder alertIconUrl(URL url) { this.alertIconUrl = url; return this; }
        public Builder confirmationRequired(boolean cr) { this.confirmationRequired = cr; return this; }
        public Builder autoCloseMillis(long ms) { this.autoCloseMillis = ms; return this; }
        public Builder confirmText(String ct) { this.confirmText = ct; return this; }
        public Builder cancelText(String ct) { this.cancelText = ct; return this; }

        public Builder asView() {
            this.modeUse = ModeUse.VIEW;
            return this;
        }

        public Builder asAlert(AlertType type) {
            this.modeUse = ModeUse.ALERT;
            this.alertType = type;
            return this;
        }

        public ViewDescriptor build() {
            // Validações de negócio
            Objects.requireNonNull(id, "id é obrigatório");
            Objects.requireNonNull(fxmlUrl, "fxmlUrl é obrigatório");
            Objects.requireNonNull(modeUse, "modeUse é obrigatório");

            if (modeUse == ModeUse.ALERT) {
                Objects.requireNonNull(alertType, "alertType obrigatório para ALERT");
                Objects.requireNonNull(modality, "modality obrigatório para ALERT");
            }

            // Retorna a chamada massiva ao construtor do Record
            return new ViewDescriptor(
                    id, fxmlUrl, origin, viewType, controllerClass, managedController,
                    cssMode, primaryCss, additionalCss, modeUse, title, width, height,
                    resizable, centered, alwaysOnTop, maximized, fullScreen, minWidth,
                    minHeight, maxWidth, maxHeight, opacity, stageStyle, iconUrl, icon,
                    sound, alertIcon, eager, loadOrder, closeOnExit, closeConfirmation,
                    initMethod, resourceBundle, styleClasses, tags, description, encoding,
                    rolesAllowed, authenticated, publishes, subscribes, alertType, modality,
                    soundUrl, alertIconUrl, confirmationRequired, autoCloseMillis,
                    confirmText, cancelText
            );
        }
    }
}