package com.ossobo.winterfx.di.scanner.models;


import com.ossobo.winterfx.di.scopes.enums.ScopeType;

import java.lang.reflect.Method;

/**
 * Representa a definição de um Bean no container.
 */
public class BeanDefinition {
    private final String name;
    private final Class<?> type;
    private final ScopeType scopeType;
    // Campos para Factory Method (Marco 6.1)
    private final Class<?> factoryClass;
    private final Method factoryMethod;

    /**
     * Construtor para beans definidos por Component Scanning (via @Component, @Service, etc.).
     */
    public BeanDefinition(String name, Class<?> type, ScopeType scopeType) {
        this.name = name;
        this.type = type;
        this.scopeType = scopeType;
        this.factoryClass = null;
        this.factoryMethod = null;
    }

    /**
     * NOVO CONSTRUTOR para beans definidos por Factory Methods (via @Configuration e @Bean).
     */
    public BeanDefinition(String name, Class<?> type, ScopeType scopeType, Class<?> factoryClass, Method factoryMethod) {
        this.name = name;
        this.type = type;
        this.scopeType = scopeType;
        this.factoryClass = factoryClass;
        this.factoryMethod = factoryMethod;
    }

    // Getters
    public String getName() { return name; }
    public Class<?> getType() { return type; }
    public ScopeType getScopeType() { return scopeType; }

    // NOVOS GETTERS
    public Class<?> getFactoryClass() { return factoryClass; }
    public Method getFactoryMethod() { return factoryMethod; }
    public boolean isFactoryMethod() { return factoryMethod != null; }

    // toString
    @Override
    public String toString() {
        String source = isFactoryMethod()
                ? "Factory: " + factoryClass.getSimpleName() + "." + factoryMethod.getName() + "()"
                : "Class: " + type.getName();

        return "BeanDefinition [name=" + name + ", type=" + type.getSimpleName() + ", scope=" + scopeType.name() + ", source=" + source + "]";
    }
}