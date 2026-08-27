package com.ossobo.winterfx.resources;

import com.ossobo.winterfx.resources.cache.ResourceCache;
import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;
import com.ossobo.winterfx.resources.descriptor.ResourceDescriptor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.ResourceType;
import com.ossobo.winterfx.resources.excecoes.ResourceNotFoundException;
import com.ossobo.winterfx.resources.guard.ResourceGuard;
import com.ossobo.winterfx.resources.resolver.ResourceResolver;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;

import java.util.List;
import java.util.Optional;

/**
 * ResourceModule v2.0
 *
 * <p>Módulo de recursos do WinterFX - Fachada (Facade) para acesso a recursos.
 * Esta é a ÚNICA porta de entrada que o restante do framework deve usar.</p>
 *
 * <p><b>Princípios:</b></p>
 * <ul>
 *   <li><b>Encapsulamento:</b> O Registry, Resolver e Guard são internos</li>
 *   <li><b>Fail-Fast:</b> Métodos {@code requireXxx()} lançam exceção se o recurso não existir</li>
 *   <li><b>Safe Access:</b> Métodos {@code findXxx()} retornam {@link Optional}</li>
 * </ul>
 *
 * @version 2.0 (23/08/2026) - API Pública desacoplada
 */
public final class ResourceModule {

    private static final System.Logger LOGGER = System.getLogger(ResourceModule.class.getName());

    private final ResourceRegistry registry;
    private final ResourceResolver resolver;
    private final ResourceGuard guard;
    private final ResourceCache<Object> cache;

    public ResourceModule(ResourceRegistry registry) {
        this.registry = registry;
        this.resolver = new ResourceResolver(registry);
        this.guard = new ResourceGuard(registry);
        this.cache = new ResourceCache<>("global");

        LOGGER.log(System.Logger.Level.INFO,
                "ResourceModule inicializado com {0} recursos",
                registry.count());
    }

    // ============================================================
    // API DE CONSULTA PÚBLICA (Fachada Desacoplada)
    // O restante do framework chama SOMENTE ESTES MÉTODOS.
    // ============================================================

    // ---------- VIEWS ----------

    /**
     * Busca uma view de forma segura (pode não existir).
     * Ideal para buscas opcionais ou menus dinâmicos.
     *
     * @param id ID da view (ex: "login", "main", "dashboard")
     * @return Optional contendo o ViewDescriptor ou vazio se não encontrado
     */
    public Optional<ViewDescriptor> findView(String id) {
        return resolver.resolveView(id);
    }

    /**
     * Busca uma view de forma obrigatória (Fail-Fast).
     * Ideal para o StageManager: se a view principal não existe, a app não deve abrir.
     *
     * @param id ID da view (ex: "login", "main", "dashboard")
     * @return ViewDescriptor completo (com URL já resolvida)
     * @throws ResourceNotFoundException se a view não for encontrada
     */
    public ViewDescriptor requireView(String id) {
        return findView(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, ResourceType.FXML));
    }

    // ---------- IMAGENS ----------

    /**
     * Busca uma imagem de forma segura.
     *
     * @param id ID da imagem (ex: "logo", "avatar-user", "icon-save")
     * @return Optional contendo o ImageDescriptor ou vazio se não encontrado
     */
    public Optional<ImageDescriptor> findImage(String id) {
        return resolver.resolveImage(id);
    }

    /**
     * Busca uma imagem de forma obrigatória (Fail-Fast).
     * Ideal para o ImageManager carregar ícones críticos da UI.
     *
     * @param id ID da imagem
     * @return ImageDescriptor completo (com URL já resolvida)
     * @throws ResourceNotFoundException se a imagem não for encontrada
     */
    public ImageDescriptor requireImage(String id) {
        return findImage(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, ResourceType.IMAGE));
    }

    // ---------- RECURSOS GENÉRICOS ----------

    /**
     * Busca qualquer recurso por ID.
     *
     * @param id ID do recurso
     * @return Optional contendo o ResourceDescriptor ou vazio se não encontrado
     */
    public Optional<ResourceDescriptor> findResource(String id) {
        return resolver.resolveDescriptor(id);
    }

    /**
     * Busca qualquer recurso por ID e tipo.
     *
     * @param id ID do recurso
     * @param type Tipo do recurso (FXML, IMAGE, CSS, SOUND, etc.)
     * @return Optional contendo o ResourceDescriptor ou vazio se não encontrado
     */
    public Optional<ResourceDescriptor> findResource(String id, ResourceType type) {
        return resolver.resolveDescriptor(id, type);
    }

    // ---------- LISTAGENS ----------

    /**
     * Lista todas as views FXML registradas.
     * Útil para construção de menus dinâmicos em tempo de execução.
     *
     * @return Lista imutável de ViewDescriptor
     */
    public List<ViewDescriptor> getAllViews() {
        return resolver.listByType(ResourceType.FXML).stream()
                .filter(ViewDescriptor.class::isInstance)
                .map(ViewDescriptor.class::cast)
                .toList();
    }

    /**
     * Lista todas as imagens registradas.
     *
     * @return Lista imutável de ImageDescriptor
     */
    public List<ImageDescriptor> getAllImages() {
        return resolver.listByType(ResourceType.IMAGE).stream()
                .filter(ImageDescriptor.class::isInstance)
                .map(ImageDescriptor.class::cast)
                .toList();
    }

    /**
     * Lista todos os recursos de um determinado tipo.
     *
     * @param type Tipo do recurso (FXML, IMAGE, CSS, SOUND, ALERT, etc.)
     * @return Lista imutável de ResourceDescriptor
     */
    public List<ResourceDescriptor> getAllByType(ResourceType type) {
        return resolver.listByType(type);
    }

    // ---------- VERIFICAÇÃO ----------

    /**
     * Verifica se um recurso existe.
     *
     * @param id ID do recurso
     * @return true se existir, false caso contrário
     */
    public boolean exists(String id) {
        return resolver.exists(id);
    }

    /**
     * Verifica se um recurso de um determinado tipo existe.
     *
     * @param id ID do recurso
     * @param type Tipo do recurso
     * @return true se existir, false caso contrário
     */
    public boolean exists(String id, ResourceType type) {
        return resolver.exists(id, type);
    }

    // ---------- CACHE ----------

    /**
     * Obtém um recurso do cache ou computa se não estiver presente.
     * Usado para cachear objetos pesados (Parent renderizado, Image decodificada).
     *
     * @param key Chave do recurso
     * @param loader Função para carregar o recurso se não estiver em cache
     * @param <T> Tipo do recurso
     * @return Recurso cacheado ou recém-carregado
     */
    public <T> java.lang.Object getFromCache(String key, java.util.function.Function<String, java.lang.Object> loader) {
        return cache.getOrCompute(key, loader);
    }

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

    // ---------- ESTATÍSTICAS ----------

    /**
     * Retorna o número total de recursos registrados.
     */
    public int getResourceCount() {
        return registry.count();
    }

    /**
     * Retorna o tamanho atual do cache.
     */
    public int getCacheSize() {
        return cache.size();
    }

    // ============================================================
    // MÉTODOS INTERNOS (NÃO EXPORTADOS PARA O RESTO DO FRAMEWORK)
    // ============================================================

    /**
     * Obtém o ResourceGuard para validações internas.
     * Apenas para uso dentro do pacote resources.
     */
    ResourceGuard getGuard() {
        return guard;
    }

    /**
     * Obtém o ResourceRegistry para operações internas.
     * Apenas para uso dentro do pacote resources.
     */
    ResourceRegistry getRegistry() {
        return registry;
    }

    @Override
    public String toString() {
        return String.format("ResourceModule[recursos=%d, cache=%d]",
                registry.count(), cache.size());
    }
}