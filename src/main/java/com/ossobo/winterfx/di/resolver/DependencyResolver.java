package com.ossobo.winterfx.di.resolver;

import com.ossobo.winterfx.anotations.Qualifier;
import com.ossobo.winterfx.di.exceptions.BeanNotFoundException;
import com.ossobo.winterfx.di.exceptions.DependencyNotRegisteredException;
import com.ossobo.winterfx.di.injection.CollectionResolver;
import com.ossobo.winterfx.di.instantiation.InstanceCreator;
import com.ossobo.winterfx.di.lifecycle.LifecycleManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.di.scopes.interfaces.ScopeInterface;
import com.ossobo.winterfx.scanner.enums.ScopeType;
import com.ossobo.winterfx.scanner.models.BeanDefinition;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolvedor central de dependências.
 * Versão 7.0 - Sem Dual-Cache, sem detecção conflitante de ciclo, Fail-Fast rigoroso.
 */
public final class DependencyResolver {

    private BeanRegistry beanRegistry;
    private ScopeManager scopeManager;
    private InstanceCreator instanceCreator;
    private LifecycleManager lifecycleManager;
    private LifecycleEventPublisher eventPublisher;
    private CollectionResolver collectionResolver;

    public DependencyResolver() {}

    public DependencyResolver(BeanRegistry beanRegistry, ScopeManager scopeManager,
                              InstanceCreator instanceCreator, LifecycleManager lifecycleManager,
                              LifecycleEventPublisher eventPublisher,
                              CollectionResolver collectionResolver) {
        this.beanRegistry = beanRegistry;
        this.scopeManager = scopeManager;
        this.instanceCreator = instanceCreator;
        this.lifecycleManager = lifecycleManager;
        this.eventPublisher = eventPublisher;
        this.collectionResolver = collectionResolver;
    }

    public void setComponentRegistry(BeanRegistry beanRegistry) { this.beanRegistry = beanRegistry; }
    public void setScopeManager(ScopeManager scopeManager) { this.scopeManager = scopeManager; }
    public void setInstanceCreator(InstanceCreator instanceCreator) { this.instanceCreator = instanceCreator; }
    public void setLifecycleManager(LifecycleManager lifecycleManager) { this.lifecycleManager = lifecycleManager; }
    public void setEventPublisher(LifecycleEventPublisher eventPublisher) { this.eventPublisher = eventPublisher; }
    public void setCollectionResolver(CollectionResolver collectionResolver) { this.collectionResolver = collectionResolver; }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) { return (T) resolve(type, null); }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type, String qualifier) { return (T) resolve(type, qualifier); }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        var def = beanRegistry.getDefinition(name);
        if (def == null) throw new BeanNotFoundException("Bean não encontrado: " + name);
        return (T) resolve(def.type(), null);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> type) { return (T) resolve(type, name); }

    @SuppressWarnings("unchecked")
    public <T> List<T> getAllBeansOfType(Class<T> type) {
        var definitions = beanRegistry.getAllDefinitionsOfType(type);
        if (definitions.isEmpty()) return List.of();
        return definitions.stream().map(def -> (T) resolve(def.type(), null)).collect(Collectors.toList());
    }

    public Object resolve(Type dependencyType, String qualifierName) {
        var rawType = extractRawType(dependencyType);

        if (rawType.equals(List.class) || rawType.equals(Set.class) || rawType.equals(Collection.class)) {
            if (dependencyType instanceof ParameterizedType pt) {
                // ✅ CORREÇÃO: Passando o qualifierName para o CollectionResolver
                return collectionResolver.resolve(dependencyType, qualifierName);
            }
            throw new IllegalArgumentException("Coleção deve ser parametrizada: " + dependencyType.getTypeName());
        }

        if (rawType.equals(Optional.class)) {
            if (dependencyType instanceof ParameterizedType pt) {
                var innerType = (Class<?>) pt.getActualTypeArguments()[0];
                try { return Optional.of(resolveAndCast(innerType, qualifierName)); }
                catch (BeanNotFoundException e) { return Optional.empty(); }
            }
            return Optional.empty();
        }

        return resolveAndCast(rawType, qualifierName);
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Type type) { return (T) resolve(type, null); }

    private Object resolveAndCast(Class<?> type, String qualifierName) {
        var definition = findDefinition(type, qualifierName);
        if (definition == null) {
            throw new BeanNotFoundException("Nenhum componente registado para: " + type.getName());
        }

        final var implType = definition.type();
        var scope = scopeManager.getScopeHandler(definition.scopeType().getName());

        @SuppressWarnings({"unchecked", "rawtypes"})
        Object result = scope.get((Class) implType, () -> {
            var aotFactory = beanRegistry.getAotFactory(implType);
            if (aotFactory != null) return aotFactory.create(this);
            if (definition.isFactoryMethod()) return createFromFactoryMethod(definition);
            return instanceCreator.createInstance(implType);
        });

        if (eventPublisher != null) {
            eventPublisher.publishEvent(type, qualifierName, DependencyLifecycleListener.LifecycleEventType.AFTER_POST_CONSTRUCT, result);
        }
        return result;
    }

    private BeanDefinition findDefinition(Class<?> type, String qualifierName) {
        if (qualifierName != null && !qualifierName.isEmpty()) {
            var def = beanRegistry.getDefinition(qualifierName);
            if (def != null && type.isAssignableFrom(def.type())) return def;
            throw new BeanNotFoundException("Qualifier '" + qualifierName + "' não encontrado para: " + type.getName());
        }

        var def = beanRegistry.getDefinition(type);
        if (def != null) return def;

        var all = beanRegistry.getAllDefinitionsOfType(type);
        if (all.size() == 1) return all.get(0);
        if (all.size() > 1) {
            throw new DependencyNotRegisteredException("Múltiplas implementações para " + type.getName() + ". Use @Primary ou @Qualifier.");
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Object createFromFactoryMethod(BeanDefinition definition) {
        var factoryClass = definition.factoryClass();
        var factoryMethod = definition.factoryMethod();
        var factoryInstance = resolveAndCast(factoryClass, null);
        try {
            var args = resolveParameters(factoryMethod);
            factoryMethod.setAccessible(true);
            var instance = factoryMethod.invoke(factoryInstance, args);
            instanceCreator.injectAndPostConstruct(instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Falha no @Bean " + factoryMethod.getName() + " de " + factoryClass.getName(), e);
        }
    }

    private Object[] resolveParameters(java.lang.reflect.Method method) {
        var params = method.getParameters();
        var args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            args[i] = resolve(params[i].getParameterizedType(), getQualifierValue(params[i]));
        }
        return args;
    }

    private Class<?> extractRawType(Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pt) return (Class<?>) pt.getRawType();
        throw new IllegalArgumentException("Tipo não suportado: " + type.getTypeName());
    }

    private String getQualifierValue(java.lang.reflect.Parameter param) {
        var qualifier = param.getAnnotation(Qualifier.class);
        return (qualifier != null && !qualifier.value().isEmpty()) ? qualifier.value() : null;
    }
}