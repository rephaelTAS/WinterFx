package com.ossobo.winterfx.resources.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 💾 ResourceCache v3.0
 * <p>
 * Cache genérico para recursos resolvidos.
 * Usa referências fortes (Hard Reference) - recursos carregados são imutáveis.
 * Thread-safe com computeIfAbsent atômico.
 * </p>
 *
 * @param <T> Tipo do recurso cacheado (Parent, Image, AudioClip, etc)
 */
public class ResourceCache<T> {

    private final Map<String, T> cache = new ConcurrentHashMap<>();
    private final String cacheName;

    /**
     * Cria um cache com nome para identificação em logs.
     */
    public ResourceCache(String cacheName) {
        this.cacheName = cacheName;
    }

    /**
     * Obtém do cache ou computa e armazena de forma ATÔMICA.
     * computeIfAbsent garante que apenas UMA thread fará a carga pesada.
     *
     * @param key Chave do recurso
     * @param loader Função para carregar o recurso se não estiver em cache
     * @return Recurso cacheado ou recém-carregado
     */
    public T getOrCompute(String key, Function<String, T> loader) {
        return cache.computeIfAbsent(key, loader);
    }

    /**
     * Armazena diretamente no cache.
     */
    public void put(String key, T value) {
        if (key != null && value != null) {
            cache.put(key, value);
        }
    }

    /**
     * Obtém do cache sem computar.
     */
    public Optional<T> get(String key) {
        return Optional.ofNullable(cache.get(key));
    }

    /**
     * Remove do cache.
     */
    public void invalidate(String key) {
        cache.remove(key);
    }

    /**
     * Limpa todo o cache.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Verifica se existe no cache.
     */
    public boolean contains(String key) {
        return cache.containsKey(key);
    }

    /**
     * Retorna o número de entradas no cache.
     */
    public int size() {
        return cache.size();
    }

    /**
     * Retorna o número de entradas válidas (igual a size() com cache forte).
     */
    public long validSize() {
        return cache.size();
    }

    public String getName() {
        return cacheName;
    }

    @Override
    public String toString() {
        return String.format("ResourceCache[%s, %d entradas]", cacheName, cache.size());
    }
}