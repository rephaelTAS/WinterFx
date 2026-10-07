package com.ossobo.winterfx.scanner.registry;

import com.ossobo.winterfx.di.aot.InstanceFactory;
import com.ossobo.winterfx.scanner.models.BeanDefinition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Catálogo central de definições de beans do WinterFX.
 * Thread-safe: usa {@link ConcurrentHashMap} para operações concorrentes.
 *
 * @see BeanDefinition
 */
public class BeanRegistry {

    private final Map<String, BeanDefinition> definitionsByName = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<String>> beanNamesByType = new ConcurrentHashMap<>();
    private final Set<String> primaryBeanNames = ConcurrentHashMap.newKeySet();
    private final Map<Class<?>, InstanceFactory<?>> aotFactoriesByType = new ConcurrentHashMap<>();

    // ============================================================
    // REGISTRO
    // ============================================================

    /**
     * Registra uma definição de bean no catálogo.
     */
    public void registerDefinition(BeanDefinition definition) {
        Objects.requireNonNull(definition, "BeanDefinition não pode ser nulo");

        String name = definition.name();
        Class<?> type = definition.type();

        if (definitionsByName.containsKey(name)) {
            throw new IllegalStateException("Bean '" + name + "' já está registrado.");
        }

        definitionsByName.put(name, definition);
        beanNamesByType.computeIfAbsent(type, k -> new ArrayList<>()).add(name);

        if (definition.primary()) {
            primaryBeanNames.add(name);
        }
    }

    // ============================================================
    // CONSULTA POR NOME
    // ============================================================

    public BeanDefinition getDefinition(String name) {
        return definitionsByName.get(name);
    }

    public BeanDefinition getDefinition(String name, Class<?> type) {
        BeanDefinition def = definitionsByName.get(name);
        if (def == null) {
            return null;
        }
        if (!type.isAssignableFrom(def.type())) {
            return null;
        }
        return def;
    }

    // ============================================================
    // CONSULTA POR TIPO
    // ============================================================

    public BeanDefinition getDefinition(Class<?> type) {
        var beanNames = beanNamesByType.get(type);

        if (beanNames != null && !beanNames.isEmpty()) {
            // Tenta encontrar @Primary exato
            for (String beanName : beanNames) {
                var def = definitionsByName.get(beanName);
                if (def != null && def.primary() && def.type() == type) {
                    return def;
                }
            }

            // Tenta encontrar qualquer @Primary
            for (String beanName : beanNames) {
                var def = definitionsByName.get(beanName);
                if (def != null && def.primary()) {
                    return def;
                }
            }

            // Retorna primeiro
            var def = definitionsByName.get(beanNames.get(0));
            if (def != null && type.isAssignableFrom(def.type())) {
                return def;
            }
        }

        // Busca por atribuição
        return definitionsByName.values().stream()
                .filter(def -> type.isAssignableFrom(def.type()))
                .findFirst()
                .orElse(null);
    }

    public List<BeanDefinition> getAllDefinitionsOfType(Class<?> type) {
        return Collections.unmodifiableList(
                definitionsByName.values().stream()
                        .filter(def -> type.isAssignableFrom(def.type()))
                        .collect(Collectors.toList())
        );
    }

    public List<String> getBeanNamesOfType(Class<?> type) {
        List<String> names = beanNamesByType.get(type);
        return names != null ? Collections.unmodifiableList(new ArrayList<>(names)) : Collections.emptyList();
    }

    // ============================================================
    // CONSULTA GERAL
    // ============================================================

    public Collection<BeanDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(definitionsByName.values());
    }

    public Set<String> getBeanNames() {
        return Collections.unmodifiableSet(definitionsByName.keySet());
    }

    /**
     * Retorna todas as classes registradas como beans.
     * Útil para processar anotações como @PropertySource.
     */
    public Set<Class<?>> getAllClasses() {
        Set<Class<?>> allClasses = new HashSet<>();
        for (BeanDefinition def : definitionsByName.values()) {
            allClasses.add(def.type());
        }
        return Collections.unmodifiableSet(allClasses);
    }

    // ============================================================
    // VERIFICAÇÃO
    // ============================================================

    /**
     * Verifica registro por TIPO EXATO (sem fallback de isAssignableFrom).
     *
     * CONTRATO: usado na idempotência do scanPropertySources. O isRegistered()
     * normal faz match por atribuição — uma SUBCLASSE já registrada faria
     * isRegistered(supertype) retornar true e suprimiria o registro real,
     * ignorando silenciosamente o @PropertySource do supertype.
     */
    public boolean isRegisteredExact(Class<?> type) {
        List<String> names = beanNamesByType.get(type);
        return names != null && !names.isEmpty();
    }

    public boolean isRegistered(Class<?> type) {
        List<String> names = beanNamesByType.get(type);
        if (names != null && !names.isEmpty()) {
            return true;
        }
        return definitionsByName.values().stream()
                .anyMatch(def -> type.isAssignableFrom(def.type()));
    }

    public boolean containsBean(String name) {
        return definitionsByName.containsKey(name);
    }

    // ============================================================
    // AOT FACTORIES
    // ============================================================

    public void registerAotFactory(Class<?> beanType, InstanceFactory<?> factory) {
        aotFactoriesByType.put(beanType, Objects.requireNonNull(factory, "factory não pode ser nulo"));
    }

    @SuppressWarnings("unchecked")
    public <T> InstanceFactory<T> getAotFactory(Class<T> beanType) {
        return (InstanceFactory<T>) aotFactoriesByType.get(beanType);
    }

    // ============================================================
    // ESTATÍSTICAS
    // ============================================================

    public int getBeanCount() {
        return definitionsByName.size();
    }

    // ============================================================
    // LIMPEZA
    // ============================================================

    public boolean removeBean(String name) {
        BeanDefinition removed = definitionsByName.remove(name);
        if (removed != null) {
            beanNamesByType.computeIfPresent(removed.type(), (k, v) -> {
                v.remove(name);
                return v.isEmpty() ? null : v;
            });
            primaryBeanNames.remove(name);
            return true;
        }
        return false;
    }

    public void clear() {
        definitionsByName.clear();
        beanNamesByType.clear();
        primaryBeanNames.clear();
        aotFactoriesByType.clear();
    }
}