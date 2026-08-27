// ImageCache.java v3.0 - 2026-08-22
// Cache com Hard Reference + LRU Eviction (Sem SoftReference)
package com.ossobo.winterfx.imagemanager.image;

import javafx.scene.image.Image;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gerenciador de cache de imagens com Hard Reference + LRU Eviction.
 *
 * <p><b>Características:</b></p>
 * <ul>
 *   <li>✅ Hard Reference: evita coleta agressiva do GC (Java 9+)</li>
 *   <li>✅ LRU Eviction: remove as imagens menos acessadas quando atinge limite</li>
 *   <li>✅ Métricas: hits, misses, evictions, hit ratio</li>
 *   <li>✅ Thread-safe: uso de ReentrantLock</li>
 * </ul>
 *
 * @version 3.0 (22/08/2026) - Removido SoftReference (causa raiz de bugs), Hard Reference
 */
public final class ImageCache {

    // Configuração
    private static final int DEFAULT_MAX_SIZE = 500;

    // Estruturas de dados - HARD REFERENCE (sem SoftReference)
    private final Map<String, Image> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> accessTimes = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > maxSize;
        }
    };

    // Controle
    private final ReentrantLock cleanupLock = new ReentrantLock();
    private final AtomicInteger hits = new AtomicInteger(0);
    private final AtomicInteger misses = new AtomicInteger(0);
    private final AtomicInteger evictions = new AtomicInteger(0);
    private volatile int maxSize = DEFAULT_MAX_SIZE;

    /**
     * Record imutável para estatísticas do cache.
     */
    public record CacheStats(
            int currentSize,
            int maxSize,
            int hits,
            int misses,
            int evictions,
            double hitRatio
    ) {
        @Override
        public String toString() {
            return String.format("CacheStats[size=%d/%d, hits=%d, misses=%d, evictions=%d, ratio=%.1f%%]",
                    currentSize, maxSize, hits, misses, evictions, hitRatio * 100);
        }
    }

    // ===== PUT =====

    /**
     * Adiciona imagem ao cache com Hard Reference.
     * Se atingir o limite máximo, aplica evicção LRU.
     */
    public void put(String key, Image image) {
        ImageUtils.validateKey(key);
        Objects.requireNonNull(image, "Image não pode ser nula");

        cleanupLock.lock();
        try {
            // Eviction check
            if (cache.size() >= maxSize) {
                performEviction();
            }

            // HARD REFERENCE: Elimina coleta agressiva do GC
            cache.put(key, image);
            accessTimes.put(key, System.currentTimeMillis());

        } finally {
            cleanupLock.unlock();
        }
    }

    // ===== GET =====

    /**
     * Recupera imagem do cache com Hard Reference.
     * Atualiza LRU em caso de sucesso.
     */
    public Optional<Image> get(String key) {
        ImageUtils.validateKey(key);

        cleanupLock.lock();
        try {
            // HARD REFERENCE: Busca direta, sem SoftReference
            var image = cache.get(key);

            if (image == null) {
                misses.incrementAndGet();
                return Optional.empty();
            }

            // Atualiza LRU
            accessTimes.put(key, System.currentTimeMillis());
            hits.incrementAndGet();

            return Optional.of(image);

        } finally {
            cleanupLock.unlock();
        }
    }

    // ===== EVICTION =====

    /**
     * Aplica política de evicção LRU.
     * Remove as imagens menos acessadas até atingir 90% do limite.
     */
    private void performEviction() {
        cleanupLock.lock();
        try {
            var targetSize = (int) (maxSize * 0.9);
            var toRemove = Math.max(1, cache.size() - targetSize);
            var removed = 0;

            while (removed < toRemove && !accessTimes.isEmpty()) {
                var oldestKey = accessTimes.keySet().iterator().next();
                cache.remove(oldestKey);
                accessTimes.remove(oldestKey);
                removed++;
                evictions.incrementAndGet();
            }

        } finally {
            cleanupLock.unlock();
        }
    }

    // ===== CONSULTA =====

    public boolean contains(String key) {
        return cache.containsKey(key);
    }

    public void clear() {
        cleanupLock.lock();
        try {
            cache.clear();
            accessTimes.clear();
            hits.set(0);
            misses.set(0);
            evictions.set(0);
        } finally {
            cleanupLock.unlock();
        }
    }

    public int size() {
        return cache.size();
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int newSize) {
        if (newSize <= 0) {
            throw new IllegalArgumentException("Tamanho máximo deve ser positivo");
        }

        cleanupLock.lock();
        try {
            this.maxSize = newSize;
            if (cache.size() > newSize) {
                performEviction();
            }
        } finally {
            cleanupLock.unlock();
        }
    }

    // ===== MÉTRICAS =====

    public CacheStats getStats() {
        var total = hits.get() + misses.get();
        var ratio = total > 0 ? (double) hits.get() / total : 0.0;

        return new CacheStats(
                cache.size(),
                maxSize,
                hits.get(),
                misses.get(),
                evictions.get(),
                ratio
        );
    }

    @Override
    public String toString() {
        var stats = getStats();
        return stats.toString();
    }
}