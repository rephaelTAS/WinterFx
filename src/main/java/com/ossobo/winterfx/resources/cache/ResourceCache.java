/*
 * ResourceCache v1.0 (OPCIONAL)
 *
 * Responsabilidade: guardar resultados já resolvidos, se fizer sentido.
 * Entrada: chave do recurso e objeto resolvido.
 * Saída: acesso rápido ao recurso já preparado.
 * Depende de: política de cache.
 */

package com.ossobo.winterfx.resources.cache;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 💾 ResourceCache v1.0 (OPCIONAL)
 * <p>
 * Cache genérico para recursos resolvidos.
 * Usa SoftReference para permitir GC sob pressão de memória.
 * </p>
 *
 * @param <T> Tipo do recurso cacheado (Parent, Image, AudioClip, etc)
 */
public class ResourceCache<T> {

    private final Map<String, SoftReference<T>> cache = new ConcurrentHashMap<>();
    private final String cacheName;

    /**
     * Cria um cache com nome para identificação em logs.
     */
    public ResourceCache(String cacheName) {
        this.cacheName = cacheName;
    }

    /**
     * Obtém do cache ou computa e armazena.
     *
     * @param key Chave do recurso
     * @param loader Função para carregar o recurso se não estiver em cache
     * @return Recurso cacheado ou recém-carregado
     */
    public T getOrCompute(String key, Function<String, T> loader) {
        SoftReference<T> ref = cache.get(key);
        T value = (ref != null) ? ref.get() : null;

        if (value != null) {
            return value;
        }

        value = loader.apply(key);

        if (value != null) {
            cache.put(key, new SoftReference<>(value));
        }

        return value;
    }

    /**
     * Armazena diretamente no cache.
     */
    public void put(String key, T value) {
        if (key != null && value != null) {
            cache.put(key, new SoftReference<>(value));
        }
    }

    /**
     * Obtém do cache sem computar.
     */
    public Optional<T> get(String key) {
        SoftReference<T> ref = cache.get(key);
        return Optional.ofNullable(ref)
                .map(SoftReference::get);
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
     * Verifica se existe no cache e está válido.
     */
    public boolean contains(String key) {
        SoftReference<T> ref = cache.get(key);
        return ref != null && ref.get() != null;
    }

    /**
     * Retorna o número de entradas no cache (incluindo referências expiradas).
     */
    public int size() {
        return cache.size();
    }

    /**
     * Retorna o número de entradas válidas (não coletadas pelo GC).
     */
    public long validSize() {
        return cache.values().stream()
                .filter(ref -> ref.get() != null)
                .count();
    }

    public String getName() {
        return cacheName;
    }

    @Override
    public String toString() {
        return String.format("ResourceCache[%s, %d/%d válidos]",
                cacheName, validSize(), cache.size());
    }
}