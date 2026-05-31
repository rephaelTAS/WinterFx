package com.ossobo.winterfx.scanner.models;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class InjectionPoint {
    private final InjectionType type;
    private final Field field;
    private final Method method;
    private final Constructor<?> constructor;

    /**
     * NOVO CONSTRUTOR para beans definidos por Factory Methods
     */
    public InjectionPoint(InjectionType type, Field field, Method method, Constructor<?> constructor) {
        this.type = type;
        this.field = field;
        this.method = method;
        this.constructor = constructor;
    }

    public InjectionType getType() {
        return type;
    }

    public Field getField() {
        return field;
    }

    public Method getMethod() {
        return method;
    }

    // Dentro de InjectionPoint.java:
    public static InjectionPoint forField(Field field) {
        return new InjectionPoint(InjectionType.FIELD, field, null, null);
    }

    public static InjectionPoint forMethod(Method method) {
        return new InjectionPoint(InjectionType.METHOD, null, method, null);
    }

    public static InjectionPoint forConstructor(Constructor<?> constructor) {
        return new InjectionPoint(InjectionType.CONSTRUCTOR, null, null, constructor);
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }
}
