package com.ossobo.winterfx.di.scopes.implementations;

import com.ossobo.winterfx.di.scopes.interfaces.ScopeInterface;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * SingletonScope v2.0
 *
 * Implementação do escopo Singleton.
 *
 * Uma única instância por tipo é mantida durante toda a vida do container.
 * Deteta dependências circulares durante a criação.
 * Suporta referências "early" para resolver ciclos de setter/field.
 *
 * Características:
 * - Thread-safe (ConcurrentHashMap)
 * - Deteção de dependências circulares no construtor
 * - Suporte a early references (injeção por setter/field em ciclos)
 * - getAllInstances() para o LifecycleManager executar @PreDestroy
 *
 * @since 2.0
 */
public final class SingletonScope implements ScopeInterface {

    /** Instâncias completamente inicializadas */
    private final Map<Class<?>, Object> singletons = new ConcurrentHashMap<>();

    /** Referências early — instâncias ainda em construção (para resolver ciclos de setter/field) */
    private final Map<Class<?>, Object> earlyInstances = new ConcurrentHashMap<>();

    /** Tipos atualmente em criação (para detetar dependências circulares) */
    private final Map<Class<?>, Boolean> inCreation = new ConcurrentHashMap<>();

    public SingletonScope() {
        // Sem inicialização necessária
    }

    // ===== ScopeInterface =====

    /**
     * Obtém ou cria a instância singleton para o tipo.
     *
     * Fluxo:
     * 1. Se já existe completamente inicializada → retorna
     * 2. Se está em criação → dependência circular → exceção
     * 3. Se existe early reference → retorna (ciclo de setter/field)
     * 4. Cria nova instância via objectFactory
     *
     * @param type          classe do bean
     * @param objectFactory factory para criar se necessário
     * @param <T>           tipo do bean
     * @return instância singleton
     * @throws IllegalStateException se detetar dependência circular
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type, Supplier<T> objectFactory) {
        // 1. Já completamente inicializada?
        Object instance = singletons.get(type);
        if (instance != null) {
            return (T) instance;
        }

        // 2. Dependência circular?
        if (inCreation.containsKey(type)) {
            throw new IllegalStateException(
                    "Dependência circular detetada para " + type.getName() +
                            ". Verifique as dependências do construtor.");
        }

        // 3. Early reference disponível? (injeção por setter/field)
        Object early = earlyInstances.get(type);
        if (early != null) {
            return (T) early;
        }

        // 4. Criar nova instância
        inCreation.put(type, Boolean.TRUE);

        try {
            T newInstance = objectFactory.get();
            inCreation.remove(type);
            singletons.put(type, newInstance);
            return newInstance;
        } catch (Exception e) {
            inCreation.remove(type);
            throw new RuntimeException("Falha ao criar instância de " + type.getName(), e);
        }
    }

    /**
     * Registra uma instância já criada manualmente.
     *
     * @param type classe do bean
     * @param bean instância a registar
     * @param <T>  tipo do bean
     */
    @Override
    public <T> void put(Class<T> type, T bean) {
        singletons.put(type, bean);
    }

    /**
     * Remove uma instância do escopo.
     *
     * @param type classe do bean a remover
     * @param <T>  tipo do bean
     */
    @Override
    public <T> void remove(Class<T> type) {
        singletons.remove(type);
        earlyInstances.remove(type);
        inCreation.remove(type);
    }

    /**
     * Destrói o escopo, libertando todas as instâncias.
     * O LifecycleManager deve invocar @PreDestroy antes de chamar este método.
     */
    @Override
    public void destroy() {
        int count = singletons.size();
        singletons.clear();
        earlyInstances.clear();
        inCreation.clear();
    }

    /**
     * Limpa apenas o estado temporário.
     * Não afeta singletons — apenas early references e estado de criação.
     */
    @Override
    public void clear() {
        earlyInstances.clear();
        inCreation.clear();
    }

    // ===== MÉTODOS DE ACESSO (para LifecycleManager) =====

    /**
     * Retorna todas as instâncias singleton para iteração do LifecycleManager.
     * Usado para invocar @PreDestroy no shutdown.
     *
     * @return mapa imutável de todas as instâncias
     */
    public Map<Class<?>, Object> getAllInstances() {
        return Collections.unmodifiableMap(singletons);
    }

    /**
     * Regista uma referência early — instância ainda em construção.
     * Usado pelo InstanceCreator para suportar injeção por setter/field em ciclos.
     *
     * @param type     classe do bean
     * @param instance instância parcialmente construída
     * @param <T>      tipo do bean
     */
    public <T> void putEarly(Class<T> type, T instance) {
        earlyInstances.put(type, instance);
    }
}