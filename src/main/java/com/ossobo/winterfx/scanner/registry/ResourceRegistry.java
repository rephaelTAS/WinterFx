package com.ossobo.winterfx.scanner.registry;

import com.ossobo.winterfx.resources.descriptor.*;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.enums.ResourceType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 🗂️ ResourceRegistry v2.0
 *
 * Catálogo central - fonte única da verdade para todos os recursos.
 *
 * <p>Armazena descritores de views (FXML), alertas, imagens, CSS, sons
 * e qualquer outro tipo de recurso registrado no sistema.</p>
 *
 * <p>Thread-safe: usa {@link ConcurrentHashMap} para operações concorrentes.</p>
 *
 * <pre>
 * Uso típico:
 *   registry.register(viewDescriptor);
 *   Optional&lt;ViewDescriptor&gt; view = registry.findViewById("usuarios");
 *   List&lt;ImageDescriptor&gt; images = registry.findAllImages();
 * </pre>
 */
public final class ResourceRegistry {

    private static final Logger LOGGER = Logger.getLogger(ResourceRegistry.class.getName());

    /** Armazenamento principal: id → descriptor */
    private final Map<String, ResourceDescriptor> descriptors = new ConcurrentHashMap<>();

    /** Contador de operações para estatísticas */
    private long registrationCount = 0;
    private long unregistrationCount = 0;

    // =============================================
    // REGISTRO
    // =============================================

    /**
     * Registra um descriptor no catálogo.
     *
     * @param descriptor Descriptor a ser registrado
     * @throws NullPointerException se descriptor for nulo
     */
    public void register(ResourceDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "Descriptor não pode ser nulo");

        String id = descriptor.getId();
        ResourceDescriptor previous = descriptors.put(id, descriptor);
        registrationCount++;

        if (previous != null) {
            LOGGER.warning(() -> String.format(
                    "⚠️ Sobrescrito recurso '%s' (tipo antigo: %s, novo: %s, origem: %s)",
                    id, previous.getResourceType(), descriptor.getResourceType(),
                    descriptor.getOrigin()
            ));
        } else {
            LOGGER.fine(() -> String.format(
                    "✅ Registrado: '%s' [%s] origem=%s",
                    id, descriptor.getResourceType(), descriptor.getOrigin()
            ));
        }
    }

    /**
     * Registra múltiplos descritores de uma vez.
     *
     * @param descriptors Descritores a serem registrados
     */
    public void registerAll(ResourceDescriptor... descriptors) {
        for (ResourceDescriptor descriptor : descriptors) {
            register(descriptor);
        }
    }

    /**
     * Registra uma lista de descritores.
     *
     * @param descriptors Lista de descritores
     */
    public void registerAll(Collection<? extends ResourceDescriptor> descriptors) {
        for (ResourceDescriptor descriptor : descriptors) {
            register(descriptor);
        }
    }

    // =============================================
    // CONSULTA GENÉRICA
    // =============================================

    /**
     * Busca um descriptor pelo ID.
     *
     * @param id Identificador do recurso
     * @return Optional com o descriptor, vazio se não encontrado
     */
    public Optional<ResourceDescriptor> findById(String id) {
        return Optional.ofNullable(descriptors.get(id));
    }

    /**
     * Busca um descriptor pelo ID e tipo.
     *
     * @param id   Identificador do recurso
     * @param type Tipo esperado
     * @return Optional com o descriptor, vazio se não encontrado ou tipo incompatível
     */
    public Optional<ResourceDescriptor> findByIdAndType(String id, ResourceType type) {
        return findById(id)
                .filter(d -> d.getResourceType() == type);
    }

    /**
     * Busca um descriptor pelo ID, tipo e origem.
     *
     * @param id     Identificador do recurso
     * @param type   Tipo esperado
     * @param origin Origem desejada
     * @return Optional com o descriptor, vazio se critérios não atendidos
     */
    public Optional<ResourceDescriptor> findByIdTypeAndOrigin(String id,
                                                              ResourceType type,
                                                              ResourceOrigin origin) {
        return findByIdAndType(id, type)
                .filter(d -> d.getOrigin() == origin);
    }

    /**
     * Verifica se um ID existe no catálogo.
     *
     * @param id Identificador do recurso
     * @return true se existir
     */
    public boolean contains(String id) {
        return descriptors.containsKey(id);
    }

    /**
     * Verifica se um ID existe e é do tipo especificado.
     *
     * @param id   Identificador do recurso
     * @param type Tipo esperado
     * @return true se existir e for do tipo correto
     */
    public boolean contains(String id, ResourceType type) {
        return findByIdAndType(id, type).isPresent();
    }

    // =============================================
    // CONSULTA TIPADA (VIEWS)
    // =============================================

    /**
     * Busca um ViewDescriptor pelo ID.
     *
     * @param viewId ID da view
     * @return Optional com o ViewDescriptor
     */
    public Optional<ViewDescriptor> findViewById(String viewId) {
        return findById(viewId)
                .filter(d -> d instanceof ViewDescriptor)
                .map(d -> (ViewDescriptor) d);
    }

    /**
     * Busca uma view FXML pelo ID (exclui alertas).
     *
     * @param viewId ID da view
     * @return Optional com o ViewDescriptor (apenas FXML)
     */
    public Optional<ViewDescriptor> findFxmlViewById(String viewId) {
        return findViewById(viewId)
                .filter(d -> d.getResourceType() == ResourceType.FXML);
    }

    /**
     * Busca um alerta pelo ID.
     *
     * @param alertId ID do alerta
     * @return Optional com o ViewDescriptor (apenas ALERT)
     */
    public Optional<ViewDescriptor> findAlertById(String alertId) {
        return findViewById(alertId)
                .filter(d -> d.getResourceType() == ResourceType.ALERT);
    }

    // =============================================
    // CONSULTA TIPADA (IMAGENS)
    // =============================================

    /**
     * Busca um ImageDescriptor pelo ID.
     *
     * @param imageId ID da imagem
     * @return Optional com o ImageDescriptor
     */
    public Optional<ImageDescriptor> findImageById(String imageId) {
        return findById(imageId)
                .filter(d -> d instanceof ImageDescriptor)
                .map(d -> (ImageDescriptor) d);
    }

    // =============================================
    // LISTAGEM
    // =============================================

    /**
     * Retorna todos os descritores registrados (cópia imutável).
     *
     * @return Lista imutável de descritores
     */
    public List<ResourceDescriptor> findAll() {
        return List.copyOf(descriptors.values());
    }

    /**
     * Retorna todos os descritores de um tipo específico.
     *
     * @param type Tipo de recurso desejado
     * @return Lista imutável de descritores
     */
    public List<ResourceDescriptor> findAllByType(ResourceType type) {
        return descriptors.values().stream()
                .filter(d -> d.getResourceType() == type)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Retorna todos os descritores de uma origem específica.
     *
     * @param origin Origem desejada
     * @return Lista imutável de descritores
     */
    public List<ResourceDescriptor> findAllByOrigin(ResourceOrigin origin) {
        return descriptors.values().stream()
                .filter(d -> d.getOrigin() == origin)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Retorna todos os descritores de um tipo e origem.
     *
     * @param type   Tipo de recurso
     * @param origin Origem desejada
     * @return Lista imutável de descritores
     */
    public List<ResourceDescriptor> findAllByTypeAndOrigin(ResourceType type,
                                                           ResourceOrigin origin) {
        return descriptors.values().stream()
                .filter(d -> d.getResourceType() == type && d.getOrigin() == origin)
                .collect(Collectors.toUnmodifiableList());
    }

    // =============================================
    // LISTAGEM TIPADA
    // =============================================

    /**
     * Retorna todas as views (FXML + ALERT) registradas.
     *
     * @return Lista imutável de ViewDescriptor
     */
    public List<ViewDescriptor> findAllViews() {
        return descriptors.values().stream()
                .filter(d -> d instanceof ViewDescriptor)
                .map(d -> (ViewDescriptor) d)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Retorna apenas views FXML (exclui alertas).
     *
     * @return Lista imutável de ViewDescriptor (FXML)
     */
    public List<ViewDescriptor> findAllFxmlViews() {
        return descriptors.values().stream()
                .filter(d -> d instanceof ViewDescriptor && d.getResourceType() == ResourceType.FXML)
                .map(d -> (ViewDescriptor) d)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Retorna todos os alertas registrados.
     *
     * @return Lista imutável de ViewDescriptor (ALERT)
     */
    public List<ViewDescriptor> findAllAlerts() {
        return descriptors.values().stream()
                .filter(d -> d instanceof ViewDescriptor && d.getResourceType() == ResourceType.ALERT)
                .map(d -> (ViewDescriptor) d)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Retorna todas as imagens registradas.
     *
     * @return Lista imutável de ImageDescriptor
     */
    public List<ImageDescriptor> findAllImages() {
        return descriptors.values().stream()
                .filter(d -> d instanceof ImageDescriptor)
                .map(d -> (ImageDescriptor) d)
                .collect(Collectors.toUnmodifiableList());
    }

    // =============================================
    // LISTAGEM DE IDs
    // =============================================

    /**
     * Retorna todos os IDs registrados.
     *
     * @return Lista imutável de IDs
     */
    public List<String> findAllIds() {
        return List.copyOf(descriptors.keySet());
    }

    /**
     * Retorna IDs de um tipo específico.
     *
     * @param type Tipo de recurso
     * @return Lista imutável de IDs
     */
    public List<String> findIdsByType(ResourceType type) {
        return descriptors.values().stream()
                .filter(d -> d.getResourceType() == type)
                .map(ResourceDescriptor::getId)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Retorna IDs de views (FXML + ALERT).
     *
     * @return Lista imutável de IDs
     */
    public List<String> findAllViewIds() {
        return descriptors.values().stream()
                .filter(d -> d instanceof ViewDescriptor)
                .map(ResourceDescriptor::getId)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Retorna IDs de imagens.
     *
     * @return Lista imutável de IDs
     */
    public List<String> findAllImageIds() {
        return descriptors.values().stream()
                .filter(d -> d instanceof ImageDescriptor)
                .map(ResourceDescriptor::getId)
                .collect(Collectors.toUnmodifiableList());
    }

    // =============================================
    // REMOÇÃO
    // =============================================

    /**
     * Remove um recurso do catálogo.
     *
     * @param id Identificador do recurso
     * @return true se o recurso foi removido
     */
    public boolean unregister(String id) {
        ResourceDescriptor removed = descriptors.remove(id);
        if (removed != null) {
            unregistrationCount++;
            LOGGER.fine(() -> "🗑️ Removido: '" + id + "' [" + removed.getResourceType() + "]");
            return true;
        }
        LOGGER.fine(() -> "⚠️ Tentativa de remover recurso inexistente: '" + id + "'");
        return false;
    }

    /**
     * Remove todos os recursos de um tipo específico.
     *
     * @param type Tipo de recurso a remover
     * @return Número de recursos removidos
     */
    public int unregisterByType(ResourceType type) {
        List<String> idsToRemove = findIdsByType(type);
        idsToRemove.forEach(this::unregister);
        return idsToRemove.size();
    }

    /**
     * Remove todos os recursos de uma origem específica.
     *
     * @param origin Origem dos recursos a remover
     * @return Número de recursos removidos
     */
    public int unregisterByOrigin(ResourceOrigin origin) {
        List<String> idsToRemove = findAllByOrigin(origin).stream()
                .map(ResourceDescriptor::getId)
                .toList();
        idsToRemove.forEach(this::unregister);
        return idsToRemove.size();
    }

    // =============================================
    // ESTATÍSTICAS
    // =============================================

    /**
     * Retorna o número total de recursos registrados.
     *
     * @return Total de recursos
     */
    public int count() {
        return descriptors.size();
    }

    /**
     * Retorna o número de recursos de um tipo específico.
     *
     * @param type Tipo de recurso
     * @return Contagem
     */
    public long countByType(ResourceType type) {
        return descriptors.values().stream()
                .filter(d -> d.getResourceType() == type)
                .count();
    }

    /**
     * Retorna o número de views (FXML + ALERT).
     *
     * @return Contagem de views
     */
    public long countViews() {
        return descriptors.values().stream()
                .filter(d -> d instanceof ViewDescriptor)
                .count();
    }

    /**
     * Retorna o número de imagens.
     *
     * @return Contagem de imagens
     */
    public long countImages() {
        return descriptors.values().stream()
                .filter(d -> d instanceof ImageDescriptor)
                .count();
    }

    /**
     * Retorna estatísticas completas do catálogo.
     *
     * @return Mapa com contagens por tipo
     */
    public Map<ResourceType, Long> getStatistics() {
        return descriptors.values().stream()
                .collect(Collectors.groupingBy(
                        ResourceDescriptor::getResourceType,
                        Collectors.counting()
                ));
    }

    /**
     * Retorna o número total de registros realizados.
     */
    public long getRegistrationCount() {
        return registrationCount;
    }

    /**
     * Retorna o número total de remoções realizadas.
     */
    public long getUnregistrationCount() {
        return unregistrationCount;
    }

    // =============================================
    // LIMPEZA
    // =============================================

    /**
     * Remove todos os recursos do catálogo.
     */
    public void clear() {
        int size = descriptors.size();
        descriptors.clear();
        LOGGER.info(() -> "🧹 Registry completamente limpo (" + size + " recursos removidos)");
    }

    /**
     * Verifica se o catálogo está vazio.
     *
     * @return true se vazio
     */
    public boolean isEmpty() {
        return descriptors.isEmpty();
    }

    // =============================================
    // UTILITÁRIOS
    // =============================================

    @Override
    public String toString() {
        Map<ResourceType, Long> stats = getStatistics();
        StringBuilder sb = new StringBuilder("ResourceRegistry[total=").append(count()).append("] {");

        stats.forEach((type, count) ->
                sb.append("\n  ").append(type).append(": ").append(count)
        );

        sb.append("\n}");
        return sb.toString();
    }

    /**
     * Imprime um relatório detalhado no log.
     */
    public void printReport() {
        LOGGER.info(() -> {
            StringBuilder report = new StringBuilder("\n");
            report.append("═══════════════════════════════════════\n");
            report.append("       RESOURCE REGISTRY REPORT        \n");
            report.append("═══════════════════════════════════════\n");
            report.append("Total: ").append(count()).append("\n");
            report.append("Registros: ").append(registrationCount).append("\n");
            report.append("Remoções: ").append(unregistrationCount).append("\n\n");

            Map<ResourceType, Long> stats = getStatistics();
            report.append("Por tipo:\n");
            stats.forEach((type, count) ->
                    report.append(String.format("  %-15s: %d%n", type, count))
            );

            report.append("\nViews:\n");
            findAllViews().forEach(v ->
                    report.append(String.format("  [%s] %-30s → %s%n",
                            v.getResourceType() == ResourceType.ALERT ? "ALERT" : "VIEW",
                            v.getId(),
                            v.getFxmlUrl()))
            );

            report.append("\nImagens:\n");
            findAllImages().forEach(img ->
                    report.append(String.format("  [%s] %-30s → %s%n",
                            img.getImageType(), img.getId(), img.getImageUrl()))
            );

            report.append("═══════════════════════════════════════\n");
            return report.toString();
        });
    }
}