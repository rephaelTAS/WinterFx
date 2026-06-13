package com.ossobo.winterfx.view.anotations;

import com.ossobo.winterfx.notifications.enums.AlertType;
import com.ossobo.winterfx.view.enums.*;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.view.floatingwindow.enums.Modality;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface RegisterView {

    String id();
    String description() default "";
    String[] tags() default {};
    String fxml();
    String resourceBundle() default "";
    String encoding() default "UTF-8";
    ResourceOrigin origin() default ResourceOrigin.APPLICATION;
    Class<?> controllerClass() default void.class;
    String initMethod() default "initialize";
    boolean managedController() default false;
    ViewType viewType() default ViewType.STATIC;
    boolean eager() default false;
    int loadOrder() default 0;
    CssMode cssMode() default CssMode.NONE;
    String primaryCss() default "";
    String[] additionalCss() default {};
    String[] styleClasses() default {};
    ModeUse modeUse() default ModeUse.VIEW;
    String title() default "";
    String icon() default "";
    int width() default 800;
    int height() default 600;
    boolean resizable() default true;
    boolean centered() default true;
    boolean alwaysOnTop() default false;
    StageStyle stageStyle() default StageStyle.DECORATED;
    AlertType alertType() default AlertType.INFO;
    Modality modality() default Modality.NONE;
    String sound() default "";
    String alertIcon() default "";
    String confirmText() default "OK";
    String cancelText() default "Cancelar";
    boolean confirmationRequired() default false;
    long autoCloseMillis() default 0;
    String[] rolesAllowed() default {};
    boolean authenticated() default false;
    String[] publishes() default {};
    String[] subscribes() default {};

}