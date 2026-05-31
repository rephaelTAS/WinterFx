package com.ossobo.winterfx.scanner;

import com.ossobo.winterfx.anotations.Inject;
import com.ossobo.winterfx.anotations.PostConstruct;
import com.ossobo.winterfx.anotations.PreDestroy;
import com.ossobo.winterfx.scanner.models.InjectionPoint;
import com.ossobo.winterfx.scanner.models.InjectionType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Extrator de metadados de beans.
 *
 * Responsabilidade: inspecionar uma classe e extrair
 * pontos de injeção, @PostConstruct e @PreDestroy.
 *
 * Usado por: ComponentScanner, DependencyResolver, ComponentRegistry
 */
public final class BeanMetadataExtractor {

    private final ReflectionScanner reflectionScanner;

    public BeanMetadataExtractor(ReflectionScanner reflectionScanner) {
        this.reflectionScanner = reflectionScanner;
    }

    /** Extrai todos os campos anotados com @Inject */
    public List<InjectionPoint> extractInjectionPoints(Class<?> type) {
        List<Field> fields = reflectionScanner.getFieldsWithAnnotation(type, Inject.class);
        List<InjectionPoint> points = new ArrayList<>();
        for (Field field : fields) {
            points.add(new InjectionPoint(InjectionType.FIELD, field, null, null));
        }
        return points;
    }

    /** Extrai o método anotado com @PostConstruct, ou null */
    public Method extractPostConstruct(Class<?> type) {
        List<Method> methods = reflectionScanner.getMethodsWithAnnotation(type, PostConstruct.class);
        return methods.isEmpty() ? null : methods.get(0);
    }

    /** Extrai o método anotado com @PreDestroy, ou null */
    public Method extractPreDestroy(Class<?> type) {
        List<Method> methods = reflectionScanner.getMethodsWithAnnotation(type, PreDestroy.class);
        return methods.isEmpty() ? null : methods.get(0);
    }
}