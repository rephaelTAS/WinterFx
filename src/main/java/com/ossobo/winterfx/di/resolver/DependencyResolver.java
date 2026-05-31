package com.ossobo.winterfx.di.resolver;

import com.ossobo.winterfx.anotations.Qualifier;
import com.ossobo.winterfx.di.aot.InstanceFactory;
import com.ossobo.winterfx.di.exceptions.BeanNotFoundException;
import com.ossobo.winterfx.di.exceptions.CircularDependencyException;
import com.ossobo.winterfx.di.exceptions.DependencyNotRegisteredException;
import com.ossobo.winterfx.di.instantiation.InstanceCreator;
import com.ossobo.winterfx.di.lifecycle.LifecycleManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.resolver.methods.CircularDependencyDetector;
import com.ossobo.winterfx.scanner.BeanMetadataExtractor;
import com.ossobo.winterfx.scanner.models.BeanDefinition;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.di.scopes.interfaces.ScopeInterface;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * DependencyResolver v3.1
 *
 * Resolve e cria beans sob demanda, gerenciando o ciclo de vida completo.
 *
 * Suporta inicialização segura via BootSequence:
 * - Construtor vazio para criação sem dependências
 * - Setters para injeção tardia de todas as dependências
 *
 * @version v3.1 (18/05/2026)
 */
public final class DependencyResolver {

    private static final Logger LOGGER = Logger.getLogger(DependencyResolver.class.getName());

    private BeanRegistry beanRegistry;
    private ScopeManager scopeManager;
    private InstanceCreator instanceCreator;
    private LifecycleManager lifecycleManager;
    private LifecycleEventPublisher eventPublisher;
    private CircularDependencyDetector dependencyDetector;
    private BeanMetadataExtractor metadataExtractor;

    // ============================================================
    // CONSTRUTORES
    // ============================================================

    /**
     * Construtor vazio — para BootSequence.
     * Dependências serão injetadas via setters.
     */
    public DependencyResolver() {
        // vazio — dependências chegam via setters
    }

    /**
     * Construtor com dependências — compatível com código existente.
     */
    public DependencyResolver(BeanRegistry beanRegistry, ScopeManager scopeManager,
                              InstanceCreator instanceCreator, LifecycleManager lifecycleManager,
                              LifecycleEventPublisher eventPublisher, CircularDependencyDetector dependencyDetector) {
        this.beanRegistry = beanRegistry;
        this.scopeManager = scopeManager;
        this.instanceCreator = instanceCreator;
        this.lifecycleManager = lifecycleManager;
        this.eventPublisher = eventPublisher;
        this.dependencyDetector = dependencyDetector;
        this.beanRegistry.setDependencyResolver(this);
    }

    // ============================================================
    // SETTERS (BootSequence — INJEÇÃO)
    // ============================================================

    /** Define o ComponentRegistry e regista este resolver nele. */
    public void setComponentRegistry(BeanRegistry beanRegistry) {
        this.beanRegistry = beanRegistry;
        this.beanRegistry.setDependencyResolver(this);
    }

    /** Define o ScopeManager após construção. */
    public void setScopeManager(ScopeManager scopeManager) {
        this.scopeManager = scopeManager;
    }

    /**
     * Define o InstanceCreator após construção.
     * Este é o setter crítico que elimina o bug "instanceCreator is null".
     */
    public void setInstanceCreator(InstanceCreator instanceCreator) {
        this.instanceCreator = instanceCreator;
    }

    /** Define o LifecycleManager após construção. */
    public void setLifecycleManager(LifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }

    /** Define o LifecycleEventPublisher após construção. */
    public void setEventPublisher(LifecycleEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /** Define o CircularDependencyDetector após construção. */
    public void setCircularDependencyDetector(CircularDependencyDetector dependencyDetector) {
        this.dependencyDetector = dependencyDetector;
    }

    public void setMetadataExtractor(BeanMetadataExtractor extractor){
        this.metadataExtractor = extractor;
    }

    // ============================================================
    // API PÚBLICA
    // ============================================================

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        return (T) resolve(type, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type, String qualifier) {
        return (T) resolve(type, qualifier);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition definition = beanRegistry.getDefinition(name);
        if (definition == null) throw new BeanNotFoundException("Bean não encontrado: " + name);
        return (T) resolve(definition.getType(), null);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> type) {
        return (T) resolve(type, name);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getAllBeansOfType(Class<T> type) {
        List<BeanDefinition> definitions = beanRegistry.getAllDefinitionsOfType(type);
        if (definitions.isEmpty()) return Collections.emptyList();
        return definitions.stream()
                .map(def -> (T) resolve(def.getType(), null))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public Object resolve(Type dependencyType, String qualifierName) {
        Class<?> rawType = extractRawType(dependencyType);
        if (rawType.equals(List.class) || rawType.equals(Set.class)) {
            if (!(dependencyType instanceof ParameterizedType pt))
                throw new IllegalArgumentException("Coleção deve ser parametrizada: " + dependencyType.getTypeName());
            return resolveCollection(pt, rawType);
        }
        if (rawType.equals(Optional.class)) {
            if (!(dependencyType instanceof ParameterizedType pt)) return Optional.empty();
            Class<?> innerType = (Class<?>) pt.getActualTypeArguments()[0];
            try { return Optional.of(resolveAndCast(innerType, qualifierName)); }
            catch (BeanNotFoundException e) { return Optional.empty(); }
        }
        return resolveAndCast(rawType, qualifierName);
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Type type) {
        return (T) resolve(type, null);
    }

    // ============================================================
    // INTERNO
    // ============================================================

    private Object resolveAndCast(Class<?> type, String qualifierName) {
        if (dependencyDetector.isResolving(type))
            throw new CircularDependencyException("Dependência circular detetada: " + type.getName());
        dependencyDetector.startResolution(type);
        try {
            BeanDefinition definition = findDefinition(type, qualifierName);
            if (definition == null)
                throw new BeanNotFoundException("Nenhum componente registado para: " + type.getName());
            final Class<?> implType = definition.getType();
            ScopeInterface scope = scopeManager.getScopeHandler(definition.getScopeType().getName());

            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object result = scope.get((Class) implType, () -> {
                InstanceFactory<?> aotFactory = beanRegistry.getAotFactory(implType);
                if (aotFactory != null) {
                    LOGGER.log(Level.FINE, "AOT factory: {0}", implType.getName());
                    return aotFactory.create(this);
                }
                if (definition.isFactoryMethod())
                    return createFromFactoryMethod(definition);
                return instanceCreator.createInstance(implType);
            });

            eventPublisher.publishEvent(type, qualifierName,
                    DependencyLifecycleListener.LifecycleEventType.AFTER_POST_CONSTRUCT, result);
            return result;
        } finally {
            dependencyDetector.endResolution(type);
        }
    }

    private BeanDefinition findDefinition(Class<?> type, String qualifierName) {
        if (qualifierName != null && !qualifierName.isEmpty()) {
            BeanDefinition def = beanRegistry.getDefinition(qualifierName);
            if (def != null && type.isAssignableFrom(def.getType())) return def;
            throw new BeanNotFoundException(
                    "Qualifier '" + qualifierName + "' não encontrado para: " + type.getName());
        }
        BeanDefinition def = beanRegistry.getDefinition(type);
        if (def != null) return def;
        List<BeanDefinition> all = beanRegistry.getAllDefinitionsOfType(type);
        if (all.size() == 1) return all.get(0);
        if (all.size() > 1) throw new DependencyNotRegisteredException(
                "Múltiplas implementações para " + type.getName() + ". Use @Primary ou @Qualifier.");

        if (!type.isInterface() && !java.lang.reflect.Modifier.isAbstract(type.getModifiers())) {
            String name = Character.toLowerCase(type.getSimpleName().charAt(0))
                    + type.getSimpleName().substring(1);



            BeanDefinition newDef = new BeanDefinition(name, type,
                    com.ossobo.winterfx.di.scopes.enums.ScopeType.SINGLETON,
                    metadataExtractor.extractInjectionPoints(type),
                    metadataExtractor.extractPostConstruct(type),
                    metadataExtractor.extractPreDestroy(type)  );
            beanRegistry.registerDefinition(newDef);
            return newDef;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object createFromFactoryMethod(BeanDefinition definition) {
        Class<?> factoryClass = definition.getFactoryClass();
        java.lang.reflect.Method factoryMethod = definition.getFactoryMethod();
        Object factoryInstance = resolveAndCast(factoryClass, null);
        try {
            Object[] args = resolveParameters(factoryMethod);
            factoryMethod.setAccessible(true);
            Object instance = factoryMethod.invoke(factoryInstance, args);
            instanceCreator.injectAndPostConstruct(instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Falha no @Bean " + factoryMethod.getName() + " de " + factoryClass.getName(), e);
        }
    }

    private Object[] resolveParameters(java.lang.reflect.Method method) {
        java.lang.reflect.Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++)
            args[i] = resolve(params[i].getParameterizedType(), getQualifierValue(params[i]));
        return args;
    }

    @SuppressWarnings("unchecked")
    private Object resolveCollection(ParameterizedType dependencyType, Class<?> collectionType) {
        Type innerType = dependencyType.getActualTypeArguments()[0];
        if (!(innerType instanceof Class<?> componentType))
            throw new IllegalArgumentException("Coleção requer tipo concreto.");
        List<BeanDefinition> definitions = beanRegistry.getAllDefinitionsOfType(componentType);
        List<Object> instances = definitions.stream()
                .map(def -> {
                    try {
                        return resolveAndCast(def.getType(), def.getName());
                    } catch (BeanNotFoundException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (collectionType.equals(Set.class)) return new HashSet<>(instances);
        return instances;
    }

    private Class<?> extractRawType(Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pt) return (Class<?>) pt.getRawType();
        throw new IllegalArgumentException("Tipo não suportado: " + type.getTypeName());
    }

    private String getQualifierValue(java.lang.reflect.Parameter param) {
        Qualifier qualifier = param
                .getAnnotation(Qualifier.class);
        if (qualifier != null && !qualifier.value().isEmpty()) return qualifier.value();
        return null;
    }
}