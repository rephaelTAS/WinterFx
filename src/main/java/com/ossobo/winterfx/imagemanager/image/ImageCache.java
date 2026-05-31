package com.ossobo.winterfx.imagemanager.image;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gerenciador de registro de imagens, responsável por mapear chaves únicas
 * para recursos de imagem (objetos Image ou caminhos de recurso).
 * Ele carrega os mapeamentos a partir de um arquivo de propriedades externo
 * na inicialização e pode ser usado para registrar imagens programaticamente.
 *
 * É um singleton gerenciado pelo DIContainer.
 *
 * 🎯 IMAGE CACHE - Com SoftReference + LRU Eviction + Métricas
 * Design Pattern: Cache com política de limpeza inteligente
 */
public class ImageCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCache.class);

    // 🔥 CONFIGURAÇÃO DO CACHE
    private static final int DEFAULT_MAX_SIZE = 500;
    private static final long CLEANUP_INTERVAL_MS = 30000; // 30 segundos

    // 🔥 ESTRUTURAS DE DADOS
    private final Map<String, SoftReference<Image>> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> accessTimes = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > maxSize;
        }
    };

    // 🔥 CONTROLE E MÉTRICAS
    private final ReentrantLock cleanupLock = new ReentrantLock();
    private final AtomicInteger hits = new AtomicInteger(0);
    private final AtomicInteger misses = new AtomicInteger(0);
    private final AtomicInteger evictions = new AtomicInteger(0);
    private volatile int maxSize = DEFAULT_MAX_SIZE;
    private volatile long lastCleanup = System.currentTimeMillis();

    public ImageCache() {
        LOGGER.info("ImageCache inicializado. Tamanho máximo: {}", maxSize);
        schedulePeriodicCleanup();
    }

    // ===== MÉTODO PRINCIPAL: PUT =====

    /**
     * ✅ ADICIONA IMAGEM COM SOFTREFERENCE + ATUALIZA LRU
     */
    public void put(String key, Image image) {
        ImageUtils.validateKey(key);
        Objects.requireNonNull(image, "Image não pode ser nula");

        cleanupLock.lock();
        try {
            // 🔥 EVICTION CHECK
            if (cache.size() >= maxSize) {
                performEviction();
            }

            // 🔥 ADICIONA COM SOFTREFERENCE
            cache.put(key, new SoftReference<>(image));
            accessTimes.put(key, System.currentTimeMillis());

            LOGGER.debug("Imagem cacheada: {} (tamanho cache: {})",
                    key, cache.size());

        } finally {
            cleanupLock.unlock();
        }
    }

    // ===== MÉTODO PRINCIPAL: GET =====

    /**
     * ✅ RECUPERA IMAGEM COM SOFTREFERENCE + ATUALIZA LRU
     */
    public Optional<Image> get(String key) {
        cleanupLock.lock();
        try {
            SoftReference<Image> ref = cache.get(key);

            if (ref == null) {
                misses.incrementAndGet();
                return Optional.empty();
            }

            Image image = ref.get();

            if (image == null) {
                // 🔥 SOFTREFERENCE FOI COLETADA PELO GC
                cache.remove(key);
                accessTimes.remove(key);
                misses.incrementAndGet();
                LOGGER.debug("SoftReference coletada para: {}", key);
                return Optional.empty();
            }

            // 🔥 ATUALIZA LRU
            accessTimes.put(key, System.currentTimeMillis());
            hits.incrementAndGet();

            return Optional.of(image);

        } finally {
            cleanupLock.unlock();
        }
    }

    // ===== MÉTODOS DE MANUTENÇÃO =====

    /**
     * 🔥 EVICTION POLICY - LRU (Least Recently Used)
     */
    private void performEviction() {
        cleanupLock.lock();
        try {
            LOGGER.debug("Iniciando eviction LRU...");
            int removed = 0;

            // REMOVE MAIS ANTIGOS PRIMEIRO
            while (cache.size() > maxSize * 0.9 && !accessTimes.isEmpty()) {
                String oldestKey = accessTimes.keySet().iterator().next();
                cache.remove(oldestKey);
                accessTimes.remove(oldestKey);
                removed++;
                evictions.incrementAndGet();
            }

            if (removed > 0) {
                LOGGER.info("Eviction LRU: {} entradas removidas", removed);
            }

        } finally {
            cleanupLock.unlock();
        }
    }

    /**
     * 🔥 LIMPEZA PERIÓDICA DE SOFTREFERENCES COLETADAS
     */
    private void performCleanup() {
        cleanupLock.lock();
        try {
            long now = System.currentTimeMillis();
            if (now - lastCleanup < CLEANUP_INTERVAL_MS) {
                return;
            }

            int beforeSize = cache.size();
            int collected = 0;

            // REMOVE ENTRIES COLETADAS PELO GC
            cache.entrySet().removeIf(entry -> {
                if (entry.getValue().get() == null) {
                    accessTimes.remove(entry.getKey());
                    return true;
                }
                return false;
            });

            collected = beforeSize - cache.size();
            lastCleanup = now;

            if (collected > 0) {
                LOGGER.debug("Cleanup: {} SoftReferences coletadas pelo GC", collected);
            }

        } finally {
            cleanupLock.unlock();
        }
    }

    /**
     * 🔥 AGENDAMENTO DE CLEANUP AUTOMÁTICO
     */
    private void schedulePeriodicCleanup() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(CLEANUP_INTERVAL_MS);
                    performCleanup();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.info("Cleanup thread interrompida");
                }
            }
        });

        cleanupThread.setDaemon(true);
        cleanupThread.setName("ImageCache-Cleanup");
        cleanupThread.start();
        LOGGER.debug("Thread de cleanup automático iniciada");
    }

    // ===== MÉTODOS DE CONSULTA E CONTROLE =====

    public boolean contains(String key) {
        return get(key).isPresent(); // Usa get() para verificar SoftReference
    }

    public void clear() {
        cleanupLock.lock();
        try {
            int size = cache.size();
            cache.clear();
            accessTimes.clear();
            hits.set(0);
            misses.set(0);
            evictions.set(0);

            LOGGER.info("Cache limpo - {} entradas removidas", size);
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
            LOGGER.info("Tamanho máximo do cache alterado para: {}", newSize);
        } finally {
            cleanupLock.unlock();
        }
    }

    // ===== MÉTRICAS DE PERFORMANCE =====

    public int getHitCount() {
        return hits.get();
    }

    public int getMissCount() {
        return misses.get();
    }

    public int getEvictionCount() {
        return evictions.get();
    }

    public double getHitRatio() {
        int total = hits.get() + misses.get();
        return total > 0 ? (double) hits.get() / total : 0.0;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("currentSize", cache.size());
        stats.put("maxSize", maxSize);
        stats.put("hits", hits.get());
        stats.put("misses", misses.get());
        stats.put("evictions", evictions.get());
        stats.put("hitRatio", String.format("%.2f%%", getHitRatio() * 100));
        stats.put("lastCleanup", lastCleanup);
        return stats;
    }

    @Override
    public String toString() {
        return String.format("ImageCache[size=%d, hits=%d, misses=%d, ratio=%.1f%%]",
                cache.size(), hits.get(), misses.get(), getHitRatio() * 100);
    }
}