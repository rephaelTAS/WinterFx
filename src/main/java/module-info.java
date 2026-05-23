/**
 * Módulo WinterFx - Framework JavaFX
 * v10.0.1
 *
 * Framework desktop com injeção automática de views, imagens e dependências via anotações.
 *
 * @author Rafael Tavares
 * @since 10.0.0
 */
module com.ossobo.winterfx {

    // =============================================
    // REQUIRES - JavaFX
    // =============================================
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive javafx.base;
    requires transitive javafx.media;

    // =============================================
    // REQUIRES - Logging
    // =============================================
    requires org.slf4j;

    // =============================================
    // REQUIRES - Scan de Classes
    // =============================================
    requires org.reflections;
    requires io.github.classgraph;

    // =============================================
    // REQUIRES - Utilities
    // =============================================
    requires java.sql;
    requires com.google.gson;
    requires com.google.common;
    requires javassist;

    // =============================================
    // REQUIRES - UI Libraries
    // =============================================
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.bootstrapfx.core;

    // =============================================
    // EXPORTS - CORE
    // =============================================
    exports com.ossobo.winterfx;
    exports com.ossobo.winterfx.bootstrap;
    exports com.ossobo.winterfx.core;
    exports com.ossobo.winterfx.exceptions;

    // =============================================
    // EXPORTS - DI CONTAINER
    // =============================================
    exports com.ossobo.winterfx.di;
    exports com.ossobo.winterfx.di.annotations;
    exports com.ossobo.winterfx.di.exceptions;
    exports com.ossobo.winterfx.di.scopes;
    exports com.ossobo.winterfx.di.scopes.enums;
    exports com.ossobo.winterfx.di.scanner;
    exports com.ossobo.winterfx.di.scanner.models;
    exports com.ossobo.winterfx.di.reflection;
    exports com.ossobo.winterfx.di.resolver;
    exports com.ossobo.winterfx.di.aot;

    // =============================================
    // EXPORTS - RESOURCES (DESCRIPTORS)
    // =============================================
    exports com.ossobo.winterfx.resources.descriptor;
    exports com.ossobo.winterfx.resources.enums;
    exports com.ossobo.winterfx.resources.excecoes;

    // =============================================
    // EXPORTS - RESOURCES (REGISTRY & RESOLVER)
    // =============================================
    exports com.ossobo.winterfx.resources.registry;
    exports com.ossobo.winterfx.resources.resolver;
    exports com.ossobo.winterfx.resources.api;
    exports com.ossobo.winterfx.resources.guard;

    // =============================================
    // EXPORTS - RESOURCES (LOADER & CACHE)
    // =============================================
    exports com.ossobo.winterfx.resources.loader;
    exports com.ossobo.winterfx.resources.cache;
    exports com.ossobo.winterfx.resources.bootstrap;

    // =============================================
    // EXPORTS - VIEW SYSTEM
    // =============================================
    exports com.ossobo.winterfx.view;
    exports com.ossobo.winterfx.view.design;
    exports com.ossobo.winterfx.view.exceptios;
    exports com.ossobo.winterfx.view.loader;
    exports com.ossobo.winterfx.view.refresh;

    // =============================================
    // EXPORTS - IMAGE MANAGER
    // =============================================
    exports com.ossobo.winterfx.ImageManager;
    exports com.ossobo.winterfx.ImageManager.image;

    // =============================================
    // EXPORTS - ALERT SYSTEM
    // =============================================
    exports com.ossobo.winterfx.AlertSystem;
    exports com.ossobo.winterfx.AlertSystem.core;
    exports com.ossobo.winterfx.AlertSystem.core.animation;
    exports com.ossobo.winterfx.AlertSystem.core.position;
    exports com.ossobo.winterfx.AlertSystem.core.ui;
    exports com.ossobo.winterfx.AlertSystem.fx;
    exports com.ossobo.winterfx.AlertSystem.model;
    exports com.ossobo.winterfx.AlertSystem.sound;

    // =============================================
    // EXPORTS - MODAL DIALOGS
    // =============================================
    exports com.ossobo.winterfx.Modaldialog;

    // =============================================
    // EXPORTS - NOTIFICATIONS
    // =============================================
    exports com.ossobo.winterfx.notifications.animation;
    exports com.ossobo.winterfx.notifications.builder;
    exports com.ossobo.winterfx.notifications.descriptor;
    exports com.ossobo.winterfx.notifications.exceptions;
    exports com.ossobo.winterfx.notifications.manager;
    exports com.ossobo.winterfx.notifications.model;
    exports com.ossobo.winterfx.notifications.position;
    exports com.ossobo.winterfx.notifications.presenter;
    exports com.ossobo.winterfx.notifications.renderer;
    exports com.ossobo.winterfx.notifications.types;

    // =============================================
    // EXPORTS - OWNER WINDOW PROVIDER
    // =============================================
    exports com.ossobo.winterfx.OwnerWindowProvider;

    // =============================================
    // ABERTURA PARA REFLEXÃO (FXML)
    // =============================================
    opens com.ossobo.winterfx.view to javafx.fxml;
    opens com.ossobo.winterfx.AlertSystem to javafx.fxml;
    opens com.ossobo.winterfx.AlertSystem.fx to javafx.fxml;
}