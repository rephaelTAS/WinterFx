package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.di.annotations.Qualifier;
import com.ossobo.winterfx.di.annotations.Value;
import com.ossobo.winterfx.di.configuration.ConfigurationManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * InjectionManager v3.1
 *
 * Responsabilidade única: injetar dependências em instâncias já criadas.
 *
 * Suporta inicialização segura via BootSequence:
 * - Construtor vazio para criação sem dependências
 * - Setters para injeção tardia de todas as dependências
 *
 * Suporta:
 * - @Inject em campos
 * - @Inject em métodos (setters)
 * - @Value para propriedades de configuração
 * - @Qualifier para escolher implementação específica
 * - Coleções (List<Interface>, Set<Interface>)
 *
 * @version v3.1 (18/05/2026)
 */
public final class InjectionManager {

    private static final Logger LOGGER = Logger.getLogger(InjectionManager.class.getName());

    private ReflectionCache reflectionCache;
    private ReflectionProcessor reflectionProcessor;
    private DependencyResolver dependencyResolver;
    private ConfigurationManager configurationManager;
    private LifecycleEventPublisher eventPublisher;

    // ============================================================
    // CONSTRUTORES
    // ============================================================

    /**
     * Construtor vazio — para BootSequence.
     * Dependências serão injetadas via setters.
     */
    public InjectionManager() {
        // vazio — dependências chegam via setters
    }

    /**
     * Construtor com dependências — compatível com código existente.
     */
    public InjectionManager(ReflectionCache reflectionCache,
                            ReflectionProcessor reflectionProcessor,
                            ConfigurationManager configurationManager,
                            LifecycleEventPublisher eventPublisher) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.configurationManager = configurationManager;
        this.eventPublisher = eventPublisher;
    }

    // ============================================================
    // SETTERS (BootSequence — INJEÇÃO)
    // ============================================================

    /**
     * Define o ReflectionCache após construção.
     */
    public void setReflectionCache(ReflectionCache reflectionCache) {
        this.reflectionCache = reflectionCache;
    }

    /**
     * Define o ReflectionProcessor após construção.
     */
    public void setReflectionProcessor(ReflectionProcessor reflectionProcessor) {
        this.reflectionProcessor = reflectionProcessor;
    }

    /**
     * Define o DependencyResolver após construção.
     * Usado pelo BootSequence na fase de INJEÇÃO.
     */
    public void setDependencyResolver(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    /**
     * Define o ConfigurationManager após construção.
     */
    public void setConfigurationManager(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    /**
     * Define o LifecycleEventPublisher após construção.
     */
    public void setEventPublisher(LifecycleEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    // ============================================================
    // INJEÇÃO PRINCIPAL
    // ============================================================

    /**
     * Injeta todas as dependências numa instância:
     * 1. @Value (propriedades)
     * 2. @Inject campos
     * 3. @Inject métodos
     *
     * @param instance instância do bean recém-criada
     */
    public void inject(Object instance) {
        if (instance == null) return;

        Class<?> type = instance.getClass();

        injectValues(instance, type);
        injectFields(instance, type);
        injectMethods(instance, type);

        eventPublisher.publishEvent(type, null,
                DependencyLifecycleListener.LifecycleEventType.AFTER_INJECTION, instance);
    }

    // ===== @Value =====

    /**
     * Injeta valores de configuração em campos anotados com @Value.
     * Suporta placeholders ${...} e valores padrão ${key:default}.
     */
    private void injectValues(Object instance, Class<?> type) {
        List<Field> fields = reflectionCache.getInjectableFields(type);

        for (Field field : fields) {
            if (field.isAnnotationPresent(Value.class)) {
                Value valueAnnotation = field.getAnnotation(Value.class);
                String expression = valueAnnotation.value();
                Object resolvedValue = resolveValue(expression, field.getType());
                reflectionProcessor.injectField(instance, field, resolvedValue);

                LOGGER.log(Level.FINE, "@Value {0}.{1} = {2}",
                        new Object[]{type.getSimpleName(), field.getName(), expression});
            }
        }
    }

    /**
     * Resolve uma expressão @Value.
     * Ex: "${app.name}" → "MyApp"
     * Ex: "${app.port:8080}" → "8080" (se não definido)
     */
    private Object resolveValue(String expression, Class<?> targetType) {
        String resolved = configurationManager.resolvePlaceholder(expression);

        if (resolved == null) return null;

        // Conversão de tipos básicos
        if (targetType == String.class) return resolved;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(resolved);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(resolved);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(resolved);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(resolved);

        return resolved;
    }

    // ===== @Inject CAMPOS =====

    /**
     * Injeta dependências em campos @Inject.
     */
    private void injectFields(Object instance, Class<?> type) {
        List<Field> fields = reflectionCache.getInjectableFields(type);

        for (Field field : fields) {
            if (field.isAnnotationPresent(Value.class)) continue; // Já tratado

            Object dependency = resolveFieldDependency(field);
            reflectionProcessor.injectField(instance, field, dependency);

            LOGGER.log(Level.FINE, "@Inject {0}.{1} ← {2}",
                    new Object[]{type.getSimpleName(), field.getName(),
                            dependency != null ? dependency.getClass().getSimpleName() : "null"});
        }
    }

    /**
     * Resolve a dependência para um campo @Inject.
     */
    private Object resolveFieldDependency(Field field) {
        Class<?> fieldType = field.getType();
        java.lang.reflect.Type genericType = field.getGenericType();

        // Coleção?
        if (Collection.class.isAssignableFrom(fieldType)) {
            return resolveCollection(genericType);
        }

        // @Qualifier?
        String qualifier = getQualifier(field);
        if (qualifier != null) {
            return dependencyResolver.getBean(fieldType, qualifier);
        }

        return dependencyResolver.getBean(fieldType);
    }

    // ===== @Inject MÉTODOS =====

    /**
     * Injeta dependências em métodos @Inject com parâmetros.
     */
    private void injectMethods(Object instance, Class<?> type) {
        List<Method> methods = reflectionCache.getInjectableMethods(type);

        for (Method method : methods) {
            Object[] args = resolveMethodParameters(method);
            reflectionProcessor.invokeMethod(instance, method, args);

            LOGGER.log(Level.FINE, "@Inject método {0}.{1}()",
                    new Object[]{type.getSimpleName(), method.getName()});
        }
    }

    /**
     * Resolve os parâmetros de um método @Inject.
     */
    private Object[] resolveMethodParameters(Method method) {
        java.lang.reflect.Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            args[i] = resolveParameter(params[i]);
        }

        return args;
    }

    /**
     * Resolve um único parâmetro de método.
     */
    private Object resolveParameter(java.lang.reflect.Parameter param) {
        Class<?> paramType = param.getType();
        java.lang.reflect.Type genericType = param.getParameterizedType();

        // Coleção?
        if (Collection.class.isAssignableFrom(paramType)) {
            return resolveCollection(genericType);
        }

        // @Qualifier?
        String qualifier = getQualifier(param);
        if (qualifier != null) {
            return dependencyResolver.getBean(paramType, qualifier);
        }

        return dependencyResolver.getBean(paramType);
    }

    // ===== COLEÇÕES =====

    /**
     * Resolve coleção de implementações (List<Interface> ou Set<Interface>).
     */
    @SuppressWarnings("unchecked")
    private Object resolveCollection(java.lang.reflect.Type collectionType) {
        if (!(collectionType instanceof java.lang.reflect.ParameterizedType pt)) {
            throw new IllegalArgumentException("Coleção deve ser genérica: " + collectionType);
        }

        Class<?> elementType = (Class<?>) pt.getActualTypeArguments()[0];
        Class<?> rawType = (Class<?>) pt.getRawType();

        List<?> implementations = dependencyResolver.getAllBeansOfType(elementType);

        if (List.class.isAssignableFrom(rawType)) {
            return implementations;
        } else if (Set.class.isAssignableFrom(rawType)) {
            return new java.util.HashSet<>(implementations);
        }

        throw new IllegalArgumentException("Tipo de coleção não suportado: " + rawType);
    }

    // ===== UTILITÁRIOS =====

    /**
     * Extrai o valor do @Qualifier de um campo.
     */
    private String getQualifier(Field field) {
        if (field.isAnnotationPresent(Qualifier.class)) {
            String value = field.getAnnotation(Qualifier.class).value();
            if (!value.isEmpty()) return value;
        }
        return null;
    }

    /**
     * Extrai o valor do @Qualifier de um parâmetro.
     */
    private String getQualifier(java.lang.reflect.Parameter param) {
        if (param.isAnnotationPresent(Qualifier.class)) {
            String value = param.getAnnotation(Qualifier.class).value();
            if (!value.isEmpty()) return value;
        }
        return null;
    }
}