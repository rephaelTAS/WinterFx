package com.ossobo.winterfx.resources;

import com.ossobo.winterfx.resources.cache.ResourceCache;
import com.ossobo.winterfx.resources.guard.ResourceGuard;
import com.ossobo.winterfx.resources.resolver.ResourceResolver;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;

/**
 * ResourceModule v1.0
 *
 * Módulo de recursos do WinterFX.
 * Centraliza a criação e configuração de todos os componentes de recursos.
 *
 * @version 1.0 (27/06/2026)
 */
public final class ResourceModule {

    private final ResourceRegistry registry;
    private final ResourceResolver resolver;
    private final ResourceGuard guard;
    private final ResourceCache<Object> cache;

    public ResourceModule(ResourceRegistry registry) {
        this.registry = registry;

        // ============================================================
        // 1. RESOLVER - Camada de serviço para acesso a recursos
        // ============================================================
        this.resolver = new ResourceResolver(registry);

        // ============================================================
        // 2. GUARD - Validação de recursos
        // ============================================================
        this.guard = new ResourceGuard(registry);


        // ============================================================
        // 4. CACHE - Cache de recursos carregados
        // ============================================================
        this.cache = new ResourceCache<>("global");
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public ResourceRegistry getRegistry() { return registry; }
    public ResourceResolver getResolver() { return resolver; }
    public ResourceGuard getGuard() { return guard; }
    public ResourceCache<Object> getCache() { return cache; }

    // ============================================================
    // MÉTODOS DE CONVENIÊNCIA
    // ============================================================


    /**
     * Invalida o cache de um recurso.
     */
    public void invalidateCache(String key) {
        cache.invalidate(key);
    }

    /**
     * Limpa todo o cache.
     */
    public void clearCache() {
        cache.clear();
    }
}