module com.ossobo.winterfx {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive javafx.base;
    requires transitive javafx.media;


    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    requires org.objectweb.asm.commons;

    requires org.slf4j;
    requires org.reflections;
    requires io.github.classgraph;
    requires java.sql;
    requires com.google.gson;
    requires com.google.common;
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.bootstrapfx.core;
    requires org.checkerframework.checker.qual;
    requires net.bytebuddy;
    requires java.instrument;

    exports com.ossobo.winterfx.bootstrap;
    exports com.ossobo.winterfx.exceptions;
    exports com.ossobo.winterfx.view.floatingwindow;
    exports com.ossobo.winterfx.di;
    exports com.ossobo.winterfx.di.annotations;
    exports com.ossobo.winterfx.di.enums;
    exports com.ossobo.winterfx.di.exceptions;
    exports com.ossobo.winterfx.di.scopes;
    exports com.ossobo.winterfx.di.scopes.enums;
    exports com.ossobo.winterfx.scanner.models;
    exports com.ossobo.winterfx.di.reflection;
    exports com.ossobo.winterfx.di.resolver;
    exports com.ossobo.winterfx.di.aot;
    exports com.ossobo.winterfx.resources.descriptor;
    exports com.ossobo.winterfx.resources.enums;
    exports com.ossobo.winterfx.resources.excecoes;
    exports com.ossobo.winterfx.scanner.registry;
    exports com.ossobo.winterfx.resources.resolver;
    exports com.ossobo.winterfx.resources.guard;
    exports com.ossobo.winterfx.resources.loader;
    exports com.ossobo.winterfx.resources.cache;
    exports com.ossobo.winterfx.view;
    exports com.ossobo.winterfx.view.design;
    exports com.ossobo.winterfx.view.exceptios;
    exports com.ossobo.winterfx.view.loader;
    exports com.ossobo.winterfx.view.refresh;
    exports com.ossobo.winterfx.imagemanager;
    exports com.ossobo.winterfx.imagemanager.image;
    exports com.ossobo.winterfx.notifications;
    exports com.ossobo.winterfx.notifications.model;
    exports com.ossobo.winterfx.notifications.controller;
    exports com.ossobo.winterfx.notifications.resolver;
    exports com.ossobo.winterfx.notifications.exceptions;
    exports com.ossobo.winterfx.notifications.registry;
    exports com.ossobo.winterfx.notifications.core;

    exports com.ossobo.winterfx.imagemanager.anotations;
    exports com.ossobo.winterfx.view.floatingwindow.anotations;
    exports com.ossobo.winterfx.notifications.anotations;
    exports com.ossobo.winterfx.view.controller;

    opens com.ossobo.winterfx.view to javafx.fxml;
    opens com.ossobo.winterfx.view.loader to javafx.fxml;
    opens com.ossobo.winterfx.notifications.controller to javafx.fxml;
    opens com.ossobo.winterfx.notifications.model to javafx.fxml;
    opens com.ossobo.winterfx.resources.descriptor to javafx.fxml;
    exports com.ossobo.winterfx.anotations;
    exports com.ossobo.winterfx.view.anotations;
    exports com.ossobo.winterfx.view.design.anotations;
    exports com.ossobo.winterfx.view.floatingwindow.enums;
    exports com.ossobo.winterfx.notifications.enums;
    exports com.ossobo.winterfx.view.enums;
    exports com.ossobo.winterfx.imagemanager.enums;
    exports com.ossobo.winterfx.scanner;


    exports com.ossobo.winterfx.di.scopes.implementations;
    exports com.ossobo.winterfx.di.lifecycle;
    exports com.ossobo.winterfx.runtime;
    exports com.ossobo.winterfx.runtime.handler;
    exports com.ossobo.winterfx.intercept;


    exports com.ossobo.winterfx.di.injection;
    exports com.ossobo.winterfx.di.instantiation;
    exports com.ossobo.winterfx.di.lifecycle.events;
    exports com.ossobo.winterfx.di.lifecycle.interfaces;
    exports com.ossobo.winterfx.di.resolver.methods;
    exports com.ossobo.winterfx.di.scopes.interfaces;
    exports com.ossobo.winterfx.di.configuration;
}