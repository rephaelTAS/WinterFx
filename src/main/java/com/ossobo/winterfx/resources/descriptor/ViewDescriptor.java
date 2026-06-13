package com.ossobo.winterfx.resources.descriptor;

import com.ossobo.winterfx.notifications.enums.AlertType;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.enums.ResourceType;
import com.ossobo.winterfx.view.enums.CssMode;
import com.ossobo.winterfx.view.enums.ModeUse;
import com.ossobo.winterfx.view.enums.StageStyle;
import com.ossobo.winterfx.view.enums.ViewType;
import com.ossobo.winterfx.view.floatingwindow.enums.Modality;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ViewDescriptor extends ResourceDescriptor {

    private final ViewType viewType;
    private final Class<?> controllerClass;
    private final boolean managedController;
    private final CssMode cssMode;
    private final URL primaryCss;
    private final List<URL> additionalCss;
    private final ModeUse modeUse;

    private final String title;
    private final int width;
    private final int height;
    private final boolean resizable;
    private final boolean centered;
    private final boolean alwaysOnTop;
    private final boolean maximized;
    private final boolean fullScreen;
    private final double minWidth;
    private final double minHeight;
    private final double maxWidth;
    private final double maxHeight;
    private final double opacity;
    private final StageStyle stageStyle;
    private final URL iconUrl;

    private final boolean eager;
    private final int loadOrder;
    private final boolean closeOnExit;
    private final String closeConfirmation;
    private final String initMethod;
    private final String resourceBundle;
    private final List<String> styleClasses;
    private final List<String> tags;
    private final String description;
    private final String encoding;
    private final List<String> rolesAllowed;
    private final boolean authenticated;
    private final List<String> publishes;
    private final List<String> subscribes;

    private final AlertType alertType;
    private final Modality modality;
    private final URL soundUrl;
    private final URL alertIconUrl;
    private final boolean confirmationRequired;
    private final long autoCloseMillis;
    private final String confirmText;
    private final String cancelText;

    private ViewDescriptor(Builder builder) {
        super(builder.id, builder.fxmlUrl,
                builder.modeUse == ModeUse.ALERT ? ResourceType.ALERT : ResourceType.FXML,
                builder.origin);

        this.viewType = Objects.requireNonNullElse(builder.viewType, ViewType.STATIC);
        this.controllerClass = builder.controllerClass;
        this.managedController = builder.managedController;
        this.cssMode = Objects.requireNonNullElse(builder.cssMode, CssMode.NONE);
        this.primaryCss = builder.primaryCss;
        this.additionalCss = builder.additionalCss != null
                ? List.copyOf(builder.additionalCss)
                : Collections.emptyList();
        this.modeUse = Objects.requireNonNull(builder.modeUse, "modeUse é obrigatório");

        this.title = builder.title != null ? builder.title : "";
        this.width = builder.width > 0 ? builder.width : 800;
        this.height = builder.height > 0 ? builder.height : 600;
        this.resizable = builder.resizable;
        this.centered = builder.centered;
        this.alwaysOnTop = builder.alwaysOnTop;
        this.maximized = builder.maximized;
        this.fullScreen = builder.fullScreen;
        this.minWidth = builder.minWidth;
        this.minHeight = builder.minHeight;
        this.maxWidth = builder.maxWidth;
        this.maxHeight = builder.maxHeight;
        this.opacity = builder.opacity > 0 ? builder.opacity : 1.0;
        this.stageStyle = Objects.requireNonNullElse(builder.stageStyle, StageStyle.DECORATED);
        this.iconUrl = builder.iconUrl;

        this.eager = builder.eager;
        this.loadOrder = builder.loadOrder;
        this.closeOnExit = builder.closeOnExit;
        this.closeConfirmation = builder.closeConfirmation;
        this.initMethod = builder.initMethod != null ? builder.initMethod : "initialize";
        this.resourceBundle = builder.resourceBundle;
        this.styleClasses = builder.styleClasses != null ? List.copyOf(builder.styleClasses) : Collections.emptyList();
        this.tags = builder.tags != null ? List.copyOf(builder.tags) : Collections.emptyList();
        this.description = builder.description != null ? builder.description : "";
        this.encoding = builder.encoding != null ? builder.encoding : "UTF-8";
        this.rolesAllowed = builder.rolesAllowed != null ? List.copyOf(builder.rolesAllowed) : Collections.emptyList();
        this.authenticated = builder.authenticated;
        this.publishes = builder.publishes != null ? List.copyOf(builder.publishes) : Collections.emptyList();
        this.subscribes = builder.subscribes != null ? List.copyOf(builder.subscribes) : Collections.emptyList();

        this.alertType = builder.alertType;
        this.modality = builder.modality;
        this.soundUrl = builder.soundUrl;
        this.alertIconUrl = builder.alertIconUrl;
        this.confirmationRequired = builder.confirmationRequired;
        this.autoCloseMillis = builder.autoCloseMillis;
        this.confirmText = builder.confirmText != null ? builder.confirmText : "OK";
        this.cancelText = builder.cancelText != null ? builder.cancelText : "Cancelar";
    }

    public ViewType getViewType() { return viewType; }
    public Class<?> getControllerClass() { return controllerClass; }
    public boolean isManagedController() { return managedController; }
    public CssMode getCssMode() { return cssMode; }
    public URL getPrimaryCss() { return primaryCss; }
    public List<URL> getAdditionalCss() { return additionalCss; }
    public ModeUse getModeUse() { return modeUse; }
    public URL getFxmlUrl() { return getUrl(); }

    public String getTitle() { return title; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isResizable() { return resizable; }
    public boolean isCentered() { return centered; }
    public boolean isAlwaysOnTop() { return alwaysOnTop; }
    public boolean isMaximized() { return maximized; }
    public boolean isFullScreen() { return fullScreen; }
    public double getMinWidth() { return minWidth; }
    public double getMinHeight() { return minHeight; }
    public double getMaxWidth() { return maxWidth; }
    public double getMaxHeight() { return maxHeight; }
    public double getOpacity() { return opacity; }
    public StageStyle getStageStyle() { return stageStyle; }
    public URL getIconUrl() { return iconUrl; }

    public boolean isEager() { return eager; }
    public int getLoadOrder() { return loadOrder; }
    public boolean isCloseOnExit() { return closeOnExit; }
    public String getCloseConfirmation() { return closeConfirmation; }
    public String getInitMethod() { return initMethod; }
    public String getResourceBundle() { return resourceBundle; }
    public List<String> getStyleClasses() { return styleClasses; }
    public List<String> getTags() { return tags; }
    public String getDescription() { return description; }
    public String getEncoding() { return encoding; }
    public List<String> getRolesAllowed() { return rolesAllowed; }
    public boolean isAuthenticated() { return authenticated; }
    public List<String> getPublishes() { return publishes; }
    public List<String> getSubscribes() { return subscribes; }

    public AlertType getAlertType() { return alertType; }
    public Modality getModality() { return modality; }
    public URL getSoundUrl() { return soundUrl; }
    public URL getAlertIconUrl() { return alertIconUrl; }
    public boolean isConfirmationRequired() { return confirmationRequired; }
    public long getAutoCloseMillis() { return autoCloseMillis; }
    public String getConfirmText() { return confirmText; }
    public String getCancelText() { return cancelText; }

    public boolean isAlert() { return modeUse == ModeUse.ALERT; }
    public boolean isView() { return modeUse == ModeUse.VIEW; }

    public static Builder builder() {
        return new Builder();
    }

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
        public Builder managedController(boolean managedController) { this.managedController = managedController; return this; }
        public Builder cssMode(CssMode cm) { this.cssMode = cm; return this; }
        public Builder primaryCss(URL css) { this.primaryCss = css; return this; }
        public Builder additionalCss(List<URL> css) { this.additionalCss = css; return this; }
        public Builder modeUse(ModeUse mu) { this.modeUse = mu; return this; }

        public Builder title(String title) { this.title = title; return this; }
        public Builder width(int width) { this.width = width; return this; }
        public Builder height(int height) { this.height = height; return this; }
        public Builder resizable(boolean resizable) { this.resizable = resizable; return this; }
        public Builder centered(boolean centered) { this.centered = centered; return this; }
        public Builder alwaysOnTop(boolean aot) { this.alwaysOnTop = aot; return this; }
        public Builder maximized(boolean max) { this.maximized = max; return this; }
        public Builder fullScreen(boolean fs) { this.fullScreen = fs; return this; }
        public Builder minWidth(double mw) { this.minWidth = mw; return this; }
        public Builder minHeight(double mh) { this.minHeight = mh; return this; }
        public Builder maxWidth(double mw) { this.maxWidth = mw; return this; }
        public Builder maxHeight(double mh) { this.maxHeight = mh; return this; }
        public Builder opacity(double opacity) { this.opacity = opacity; return this; }
        public Builder stageStyle(StageStyle ss) { this.stageStyle = ss; return this; }
        public Builder iconUrl(URL iconUrl) { this.iconUrl = iconUrl; return this; }

        public Builder eager(boolean eager) { this.eager = eager; return this; }
        public Builder loadOrder(int order) { this.loadOrder = order; return this; }
        public Builder closeOnExit(boolean coe) { this.closeOnExit = coe; return this; }
        public Builder closeConfirmation(String cc) { this.closeConfirmation = cc; return this; }
        public Builder initMethod(String method) { this.initMethod = method; return this; }
        public Builder resourceBundle(String rb) { this.resourceBundle = rb; return this; }
        public Builder styleClasses(List<String> sc) { this.styleClasses = sc; return this; }
        public Builder tags(List<String> tags) { this.tags = tags; return this; }
        public Builder description(String desc) { this.description = desc; return this; }
        public Builder encoding(String encoding) { this.encoding = encoding; return this; }
        public Builder rolesAllowed(List<String> rolesAllowed) { this.rolesAllowed = rolesAllowed; return this; }
        public Builder authenticated(boolean authenticated) { this.authenticated = authenticated; return this; }
        public Builder publishes(List<String> publishes) { this.publishes = publishes; return this; }
        public Builder subscribes(List<String> subscribes) { this.subscribes = subscribes; return this; }

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
            Objects.requireNonNull(id, "id é obrigatório");
            Objects.requireNonNull(fxmlUrl, "fxmlUrl é obrigatório");
            Objects.requireNonNull(modeUse, "modeUse é obrigatório");

            if (modeUse == ModeUse.ALERT) {
                Objects.requireNonNull(alertType, "alertType é obrigatório para ALERT");
                Objects.requireNonNull(modality, "modality é obrigatório para ALERT");
            }

            return new ViewDescriptor(this);
        }
    }
}