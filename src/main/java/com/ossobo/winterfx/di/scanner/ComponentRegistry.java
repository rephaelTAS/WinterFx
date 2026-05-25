package com.ossobo.winterfx.di.scanner;

import com.ossobo.winterfx.di.aot.InstanceFactory;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.di.scanner.models.BeanDefinition;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.di.scopes.enums.ScopeType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ComponentRegistry v2.0
 * <p>
 * Registo central de definições de beans.
 * <p>
 * Suporta:
 * - Beans por nome e por tipo
 * - @Primary (primeiro registado ganha)
 * - Factory methods (@Configuration + @Bean)
 * - AOT factories (Marco VII)
 * - Registro manual de instâncias (para Bootstrap)
 */
public class ComponentRegistry {

    private final Map<String, BeanDefinition> definitionsByName = new ConcurrentHashMap<>();
    private final Map<Class<?>, String> primaryBeanNamesByType = new ConcurrentHashMap<>();
    private final Map<Class<?>, InstanceFactory<?>> aotFactoriesByType = new ConcurrentHashMap<>();

    private DependencyResolver dependencyResolver;

    public void setDependencyResolver(DependencyResolver resolver) {
        this.dependencyResolver = resolver;
    }

    // ===== REGISTO =====

    public void registerDefinition(BeanDefinition definition) {
        if (definitionsByName.containsKey(definition.getName())) {
            throw new IllegalStateException(
                    "Bean '" + definition.getName() + "' já está registado.");
        }
        definitionsByName.put(definition.getName(), definition);
        primaryBeanNamesByType.putIfAbsent(definition.getType(), definition.getName());
    }

    /**
     * Regista uma instância já criada (Bootstrap, serviços externos).
     */
    public <T> void registerInstance(Class<T> type, T instance, ScopeManager scopeManager) {
        String name = Character.toLowerCase(type.getSimpleName().charAt(0))
                + type.getSimpleName().substring(1);
        BeanDefinition definition = new BeanDefinition(name, type, ScopeType.SINGLETON);
        registerDefinition(definition);

        // Regista a instância no escopo Singleton
        scopeManager.getSingletonScope().put(type, instance);
    }

    // ===== CONSULTA =====

    public BeanDefinition getDefinition(String name) {
        return definitionsByName.get(name);
    }

    public BeanDefinition getDefinition(Class<?> type) {
        String beanName = primaryBeanNamesByType.get(type);
        if (beanName != null) {
            return definitionsByName.get(beanName);
        }
        return definitionsByName.values().stream()
                .filter(def -> type.isAssignableFrom(def.getType()))
                .findFirst()
                .orElse(null);
    }

    public List<BeanDefinition> getAllDefinitionsOfType(Class<?> type) {
        return Collections.unmodifiableList(
                definitionsByName.values().stream()
                        .filter(def -> type.isAssignableFrom(def.getType()))
                        .collect(Collectors.toList())
        );
    }

    /**
     * Retorna TODAS as definições de beans registradas.
     * 🔥 USADO POR: DiContainer.findClassesWithAnnotation()
     *              DiContainer.findMethodsWithAnnotation()
     *
     * @return Coleção imutável de todas as BeanDefinitions
     */
    public Collection<BeanDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(definitionsByName.values());
    }

    // ===== AOT =====

    public void registerAotFactory(Class<?> beanType, InstanceFactory<?> factory) {
        aotFactoriesByType.put(beanType, factory);
    }

    @SuppressWarnings("unchecked")
    public <T> InstanceFactory<T> getAotFactory(Class<T> beanType) {
        return (InstanceFactory<T>) aotFactoriesByType.get(beanType);
    }

    // ===== UTILITÁRIOS =====

    public Set<String> getBeanNames() {
        return Collections.unmodifiableSet(definitionsByName.keySet());
    }

    public boolean isRegistered(Class<?> type) {
        return primaryBeanNamesByType.containsKey(type)
                || definitionsByName.values().stream()
                .anyMatch(def -> type.isAssignableFrom(def.getType()));
    }

    public void clear() {
        definitionsByName.clear();
        primaryBeanNamesByType.clear();
        aotFactoriesByType.clear();
    }
}