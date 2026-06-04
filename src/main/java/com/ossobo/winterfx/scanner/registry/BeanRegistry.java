package com.ossobo.winterfx.scanner.registry;

import com.ossobo.winterfx.di.aot.InstanceFactory;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.scanner.models.BeanDefinition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Catálogo central de definições de beans do WinterFX.
 *
 * <p>Armazena todas as {@link BeanDefinition} descobertas pelo scanner.
 * Thread-safe: usa {@link ConcurrentHashMap} para operações concorrentes.</p>
 *
 * <p>Suporta:</p>
 * <ul>
 *   <li>Registro por nome e por tipo</li>
 *   <li>@Primary (bean marcado explicitamente como primário)</li>
 *   <li>Factory methods (@Configuration + @Bean)</li>
 *   <li>AOT factories para criação antecipada</li>
 *   <li>Múltiplos beans do mesmo tipo com @Qualifier</li>
 * </ul>
 *
 * <p><b>Comportamento de @Primary:</b></p>
 * <ul>
 *   <li>Se nenhum bean é @Primary, o primeiro registrado é usado</li>
 *   <li>Se múltiplos são @Primary, o primeiro @Primary é usado</li>
 *   <li>Bean @Primary tem prioridade sobre beans normais</li>
 * </ul>
 *
 * @see BeanDefinition
 * @see DependencyResolver
 */
public class BeanRegistry {

    private final Map<String, BeanDefinition> definitionsByName = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<String>> beanNamesByType = new ConcurrentHashMap<>();
    private final Set<String> primaryBeanNames = new HashSet<>();
    private final Map<Class<?>, InstanceFactory<?>> aotFactoriesByType = new ConcurrentHashMap<>();

    private DependencyResolver dependencyResolver;

    /**
     * Define o resolver de dependências para uso posterior.
     *
     * @param resolver resolver de dependências
     */
    public void setDependencyResolver(DependencyResolver resolver) {
        this.dependencyResolver = resolver;
    }

    public DependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    /**
     * Registra uma definição de bean no catálogo.
     *
     * <p>LANÇA exceção se já existir um bean com o mesmo nome.</p>
     *
     * @param definition definição do bean (não pode ser nulo)
     * @throws IllegalStateException se já existir um bean com o mesmo nome
     * @throws NullPointerException se definition for nulo
     */
    public void registerDefinition(BeanDefinition definition) {
        if (definition == null) {
            throw new NullPointerException("BeanDefinition não pode ser nulo");
        }

        String name = definition.getName();
        Class<?> type = definition.getType();

        if (definitionsByName.containsKey(name)) {
            throw new IllegalStateException("Bean '" + name + "' já está registrado.");
        }

        definitionsByName.put(name, definition);
        beanNamesByType.computeIfAbsent(type, k -> new ArrayList<>()).add(name);

        if (definition.isPrimary()) {
            primaryBeanNames.add(name);
        }
    }

    /**
     * Registra uma instância já criada (bootstrap, serviços externos).
     *
     * @param type tipo do bean
     * @param instance instância do bean
     * @param definition definição do bean
     * @param scopeManager gerenciador de escopos
     * @param <T> tipo do bean
     */
    public <T> void registerInstance(Class<T> type, T instance, BeanDefinition definition, ScopeManager scopeManager) {
        registerDefinition(definition);
        scopeManager.getSingletonScope().put(type, instance);
    }

    /**
     * Busca definição pelo nome do bean.
     *
     * @param name nome do bean
     * @return definição do bean, ou null se não encontrado
     */
    public BeanDefinition getDefinition(String name) {
        return definitionsByName.get(name);
    }

    /**
     * Busca definição pelo tipo do bean.
     *
     * <p>Prioridade:</p>
     * <ol>
     *   <li>Bean @Primary exato do tipo</li>
     *   <li>Primeiro bean @Primary compatível</li>
     *   <li>Primeiro bean compatível</li>
     * </ol>
     *
     * @param type tipo do bean
     * @return definição do bean, ou null se não encontrado
     */
    public BeanDefinition getDefinition(Class<?> type) {
        List<String> beanNames = beanNamesByType.get(type);

        if (beanNames != null && !beanNames.isEmpty()) {
            // Tenta encontrar @Primary exato
            for (String beanName : beanNames) {
                BeanDefinition def = definitionsByName.get(beanName);
                if (def != null && def.isPrimary() && def.getType() == type) {
                    return def;
                }
            }

            // Tenta encontrar qualquer @Primary
            for (String beanName : beanNames) {
                BeanDefinition def = definitionsByName.get(beanName);
                if (def != null && def.isPrimary()) {
                    return def;
                }
            }

            // Retorna primeiro
            BeanDefinition def = definitionsByName.get(beanNames.get(0));
            if (def != null && type.isAssignableFrom(def.getType())) {
                return def;
            }
        }

        // Busca por atribuição
        return definitionsByName.values().stream()
                .filter(def -> type.isAssignableFrom(def.getType()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Busca definição pelo nome e tipo.
     *
     * @param name nome do bean
     * @param type tipo esperado
     * @return definição do bean, ou null se não encontrado ou tipo incompatível
     */
    public BeanDefinition getDefinition(String name, Class<?> type) {
        BeanDefinition def = definitionsByName.get(name);
        if (def == null) {
            return null;
        }
        if (!type.isAssignableFrom(def.getType())) {
            return null;
        }
        return def;
    }

    /**
     * Retorna todas as definições de um tipo específico.
     *
     * @param type tipo do bean
     * @return lista imutável de definições
     */
    public List<BeanDefinition> getAllDefinitionsOfType(Class<?> type) {
        return Collections.unmodifiableList(
                definitionsByName.values().stream()
                        .filter(def -> type.isAssignableFrom(def.getType()))
                        .collect(Collectors.toList())
        );
    }

    /**
     * Retorna todas as definições registradas.
     *
     * @return coleção imutável de definições
     */
    public Collection<BeanDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(definitionsByName.values());
    }

    /**
     * Retorna todos os nomes de beans do tipo especificado.
     *
     * @param type tipo do bean
     * @return lista imutável de nomes
     */
    public List<String> getBeanNamesOfType(Class<?> type) {
        List<String> names = beanNamesByType.get(type);
        return names != null ? Collections.unmodifiableList(new ArrayList<>(names)) : Collections.emptyList();
    }

    /**
     * Registra uma factory AOT para um tipo de bean.
     *
     * @param beanType tipo do bean
     * @param factory factory AOT
     */
    public void registerAotFactory(Class<?> beanType, InstanceFactory<?> factory) {
        aotFactoriesByType.put(beanType, Objects.requireNonNull(factory, "factory não pode ser nulo"));
    }

    /**
     * Obtém a factory AOT para um tipo de bean.
     *
     * @param beanType tipo do bean
     * @param <T> tipo do bean
     * @return factory AOT, ou null se não existir
     */
    @SuppressWarnings("unchecked")
    public <T> InstanceFactory<T> getAotFactory(Class<T> beanType) {
        return (InstanceFactory<T>) aotFactoriesByType.get(beanType);
    }

    /**
     * Retorna todos os nomes de beans registrados.
     *
     * @return conjunto imutável de nomes
     */
    public Set<String> getBeanNames() {
        return Collections.unmodifiableSet(definitionsByName.keySet());
    }

    /**
     * Verifica se um tipo está registrado.
     *
     * @param type tipo do bean
     * @return true se há pelo menos um bean desse tipo
     */
    public boolean isRegistered(Class<?> type) {
        List<String> names = beanNamesByType.get(type);
        if (names != null && !names.isEmpty()) {
            return true;
        }
        return definitionsByName.values().stream()
                .anyMatch(def -> type.isAssignableFrom(def.getType()));
    }

    /**
     * Verifica se um bean com o nome especificado existe.
     *
     * @param name nome do bean
     * @return true se o bean existe
     */
    public boolean containsBean(String name) {
        return definitionsByName.containsKey(name);
    }

    /**
     * Retorna o número total de beans registrados.
     *
     * @return número de beans
     */
    public int getBeanCount() {
        return definitionsByName.size();
    }

    /**
     * Remove todas as definições.
     */
    public void clear() {
        definitionsByName.clear();
        beanNamesByType.clear();
        primaryBeanNames.clear();
        aotFactoriesByType.clear();
    }

    /**
     * Remove uma definição pelo nome.
     *
     * @param name nome do bean
     * @return true se removido
     */
    public boolean removeBean(String name) {
        BeanDefinition removed = definitionsByName.remove(name);
        if (removed != null) {
            beanNamesByType.computeIfPresent(removed.getType(), (k, v) -> {
                v.remove(name);
                return v.isEmpty() ? null : v;
            });
            primaryBeanNames.remove(name);
            return true;
        }
        return false;
    }
}