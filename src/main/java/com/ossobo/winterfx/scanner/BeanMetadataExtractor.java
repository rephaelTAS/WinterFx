package com.ossobo.winterfx.scanner;

import com.ossobo.winterfx.anotations.Inject;
import com.ossobo.winterfx.anotations.PostConstruct;
import com.ossobo.winterfx.anotations.PreDestroy;
import com.ossobo.winterfx.anotations.Value;
import com.ossobo.winterfx.scanner.models.InjectionPoint;
import com.ossobo.winterfx.scanner.models.InjectionType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Extrator de metadados de beans.
 *
 * <p>Responsabilidade: inspecionar uma classe e extrair pontos de injeção
 * ({@code @Inject}), valores de configuração ({@code @Value}),
 * método de inicialização ({@code @PostConstruct})
 * e método de destruição ({@code @PreDestroy}).</p>
 *
 * <p>Suporta injeção em:</p>
 * <ul>
 *   <li>Campos anotados com {@code @Inject}</li>
 *   <li>C construtores anotados com {@code @Inject}</li>
 *   <li>Métodos anotados com {@code @Inject}</li>
 *   <li>Parâmetros de construtor/método com {@code @Inject}</li>
 * </ul>
 *
 * <p>Extrai também {@code @Value} em campos para injeção de propriedades.</p>
 *
 * @see InjectionPoint
 * @see Value
 */
public final class BeanMetadataExtractor {

    private final ReflectionScanner reflectionScanner;

    public BeanMetadataExtractor(ReflectionScanner reflectionScanner) {
        this.reflectionScanner = reflectionScanner;
    }

    /**
     * Extrai todos os pontos de injeção: campos, métodos e construtores anotados com {@code @Inject}.
     *
     * <p>Ordem de extração:</p>
     * <ol>
     *   <li>Construtores com {@code @Inject}</li>
     *   <li>Campos com {@code @Inject}</li>
     *   <li>Métodos com {@code @Inject}</li>
     * </ol>
     *
     * @param type classe a ser inspecionada
     * @return lista de pontos de injeção
     */
    public List<InjectionPoint> extractInjectionPoints(Class<?> type) {
        List<InjectionPoint> points = new ArrayList<>();

        // Construtores @Inject primeiro (prioridade em DI)
        for (Constructor<?> ctor : reflectionScanner.getConstructors(type)) {
            if (ctor.isAnnotationPresent(Inject.class)) {
                points.add(InjectionPoint.forConstructor(ctor));
            }
        }

        // Campos @Inject
        for (Field field : reflectionScanner.getFieldsWithAnnotation(type, Inject.class)) {
            points.add(InjectionPoint.forField(field));
        }

        // Métodos @Inject (setters)
        for (Method method : reflectionScanner.getMethodsWithAnnotation(type, Inject.class)) {
            points.add(InjectionPoint.forMethod(method));
        }

        return points;
    }

    /**
     * Extrai campos anotados com {@code @Value} para injeção de propriedades.
     *
     * @param type classe a ser inspecionada
     * @return mapa de fieldName → expressão (ex: "appName" → "${app.name}")
     */
    public Map<String, String> extractValues(Class<?> type) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Field field : reflectionScanner.getFieldsWithAnnotation(type, Value.class)) {
            Value v = field.getAnnotation(Value.class);
            values.put(field.getName(), v.value());
        }
        return values;
    }

    /**
     * Extrai método com {@code @PostConstruct} para inicialização do bean.
     *
     * @param type classe a ser inspecionada
     * @return método de inicialização, ou null se não existir
     */
    public Method extractPostConstruct(Class<?> type) {
        List<Method> methods = reflectionScanner.getMethodsWithAnnotation(type, PostConstruct.class);
        return methods.isEmpty() ? null : methods.get(0);
    }

    /**
     * Extrai método com {@code @PreDestroy} para limpeza do bean.
     *
     * @param type classe a ser inspecionada
     * @return método de destruição, ou null se não existir
     */
    public Method extractPreDestroy(Class<?> type) {
        List<Method> methods = reflectionScanner.getMethodsWithAnnotation(type, PreDestroy.class);
        return methods.isEmpty() ? null : methods.get(0);
    }

    /**
     * Extrai parâmetros de construtor anotados com {@code @Inject}.
     *
     * @param constructor construtor a ser inspecionado
     * @return lista de InjectionPoint para cada parâmetro
     */
    public List<InjectionPoint> extractConstructorParameters(Constructor<?> constructor) {
        List<InjectionPoint> points = new ArrayList<>();
        Parameter[] parameters = constructor.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (param.isAnnotationPresent(Inject.class)) {
                points.add(InjectionPoint.forConstructorParameter(constructor, i, param));
            }
        }

        return points;
    }

    /**
     * Extrai parâmetros de método anotados com {@code @Inject}.
     *
     * @param method método a ser inspecionado
     * @return lista de InjectionPoint para cada parâmetro
     */
    public List<InjectionPoint> extractMethodParameters(Method method) {
        List<InjectionPoint> points = new ArrayList<>();
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (param.isAnnotationPresent(Inject.class)) {
                points.add(InjectionPoint.forMethodParameter(method, i, param));
            }
        }

        return points;
    }
}