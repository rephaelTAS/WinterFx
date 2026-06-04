package com.ossobo.winterfx.scanner.registry;

import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;
import com.ossobo.winterfx.resources.descriptor.ResourceDescriptor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.enums.ResourceType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Catálogo central de recursos do WinterFX.
 *
 * <p>Armazena descritores de views (FXML), alertas, imagens e qualquer outro
 * tipo de recurso registrado no sistema.</p>
 *
 * <p>Thread-safe: usa {@link ConcurrentHashMap}.</p>
 *
 * <p>Suporta:</p>
 * <ul>
 *   <li>Registro por ID único</li>
 *   <li>Sobrescrita automática se já existir (último ganha)</li>
 *   <li>Filtragem por tipo (FXML, IMAGE, ALERT, etc.)</li>
 *   <li>Filtragem por origem (APPLICATION, MODULE, EXTERNAL)</li>
 *   <li>Estatísticas de recursos por tipo</li>
 *   <li>Registro em massa</li>
 * </ul>
 *
 * @see ResourceDescriptor
 * @see ViewDescriptor
 * @see ImageDescriptor
 */
public final class ResourceRegistry {

    private final Map<String, ResourceDescriptor> descriptors = new ConcurrentHashMap<>();
    private final AtomicLong registrationCount = new AtomicLong(0);
    private final AtomicLong unregistrationCount = new AtomicLong(0);
    private final AtomicLong overwriteCount = new AtomicLong(0);

    /**
     * Registra um descritor no catálogo.
     *
     * <p><b>Comportamento:</b> Se já existir recurso com mesmo ID, sobrescreve automaticamente (último ganha).</p>
     *
     * @param descriptor descritor do recurso (não pode ser nulo)
     * @throws NullPointerException se descriptor for nulo
     * @throws IllegalArgumentException se ID for nulo ou vazio
     */
    public void register(ResourceDescriptor descriptor) {
        if (descriptor == null) {
            throw new NullPointerException("Descriptor não pode ser nulo");
        }

        String id = descriptor.getId();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID do descriptor não pode ser nulo ou vazio");
        }

        // ⭐ Se já existe, sobrescreve (comportamento de "último ganha")
        boolean wasOverwritten = descriptors.put(id, descriptor) != null;
        registrationCount.incrementAndGet();

        if (wasOverwritten) {
            overwriteCount.incrementAndGet();
        }
    }

    /**
     * Registra múltiplos descritores.
     *
     * @param descriptors descritores do recurso
     */
    public void registerAll(ResourceDescriptor... descriptors) {
        for (ResourceDescriptor descriptor : descriptors) {
            register(descriptor);
        }
    }

    /**
     * Registra uma coleção de descritores.
     *
     * @param descriptors coleção de descritores
     */
    public void registerAll(Collection<? extends ResourceDescriptor> descriptors) {
        for (ResourceDescriptor descriptor : descriptors) {
            register(descriptor);
        }
    }

    /**
     * Busca descritor pelo ID.
     *
     * @param id ID do recurso
     * @return Optional com o descritor, ou Optional.empty se não encontrado
     */
    public Optional<ResourceDescriptor> findById(String id) {
        return Optional.ofNullable(descriptors.get(id));
    }

    /**
     * Busca descritor pelo ID e tipo.
     *
     * @param id ID do recurso
     * @param type tipo do recurso
     * @return Optional com o descritor, ou Optional.empty se não encontrado ou tipo incompatível
     */
    public Optional<ResourceDescriptor> findByIdAndType(String id, ResourceType type) {
        return findById(id).filter(d -> d.getResourceType() == type);
    }

    /**
     * Busca descritor pelo ID, tipo e origem.
     *
     * @param id ID do recurso
     * @param type tipo do recurso
     * @param origin origem do recurso
     * @return Optional com o descritor, ou Optional.empty se não encontrado ou filtros incompatíveis
     */
    public Optional<ResourceDescriptor> findByIdTypeAndOrigin(String id, ResourceType type, ResourceOrigin origin) {
        return findByIdAndType(id, type).filter(d -> d.getOrigin() == origin);
    }

    /**
     * Verifica se um recurso com o ID existe.
     *
     * @param id ID do recurso
     * @return true se existe
     */
    public boolean contains(String id) {
        return descriptors.containsKey(id);
    }

    /**
     * Verifica se um recurso com o ID e tipo existe.
     *
     * @param id ID do recurso
     * @param type tipo do recurso
     * @return true se existe com o tipo especificado
     */
    public boolean contains(String id, ResourceType type) {
        return findByIdAndType(id, type).isPresent();
    }

    /**
     * Busca ViewDescriptor pelo ID.
     *
     * @param viewId ID da view
     * @return Optional com ViewDescriptor, ou Optional.empty se não encontrado ou não for ViewDescriptor
     */
    public Optional<ViewDescriptor> findViewById(String viewId) {
        return findById(viewId)
                .filter(d -> d instanceof ViewDescriptor)
                .map(d -> (ViewDescriptor) d);
    }

    /**
     * Busca ViewDescriptor FXML pelo ID.
     *
     * @param viewId ID da view
     * @return Optional com ViewDescriptor FXML, ou Optional.empty se não encontrado
     */
    public Optional<ViewDescriptor> findFxmlViewById(String viewId) {
        return findViewById(viewId)
                .filter(d -> d.getResourceType() == ResourceType.FXML);
    }

    /**
     * Busca ViewDescriptor de alerta pelo ID.
     *
     * @param alertId ID do alerta
     * @return Optional com ViewDescriptor de alerta, ou Optional.empty se não encontrado
     */
    public Optional<ViewDescriptor> findAlertById(String alertId) {
        return findById(alertId)
                .filter(d -> d instanceof ViewDescriptor)
                .map(d -> (ViewDescriptor) d)
                .filter(d -> d.getResourceType() == ResourceType.ALERT);
    }

    /**
     * Busca ImageDescriptor pelo ID.
     *
     * @param imageId ID da imagem
     * @return Optional com ImageDescriptor, ou Optional.empty se não encontrado
     */
    public Optional<ImageDescriptor> findImageById(String imageId) {
        return findById(imageId)
                .filter(d -> d instanceof ImageDescriptor)
                .map(d -> (ImageDescriptor) d);
    }

    /**
     * Retorna todos os recursos registrados.
     *
     * @return lista imutável de descritores
     */
    public List<ResourceDescriptor> findAll() {
        return List.copyOf(descriptors.values());
    }

    /**
     * Retorna todos os recursos de um tipo específico.
     *
     * @param type tipo do recurso
     * @return lista imutável de descritores
     */
    public List<ResourceDescriptor> findAllByType(ResourceType type) {
        return descriptors.values().stream()
                .filter(d -> d.getResourceType() == type)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Retorna todas as views registradas.
     *
     * @return lista imutável de ViewDescriptors
     */
    public List<ViewDescriptor> findAllViews() {
        return descriptors.values().stream()
                .filter(d -> d instanceof ViewDescriptor)
                .map(d -> (ViewDescriptor) d)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Retorna todas as views FXML registradas.
     *
     * @return lista imutável de ViewDescriptors FXML
     */
    public List<ViewDescriptor> findAllFxmlViews() {
        return descriptors.values().stream()
                .filter(d -> d instanceof ViewDescriptor && d.getResourceType() == ResourceType.FXML)
                .map(d -> (ViewDescriptor) d)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Retorna todas as imagens registradas.
     *
     * @return lista imutável de ImageDescriptors
     */
    public List<ImageDescriptor> findAllImages() {
        return descriptors.values().stream()
                .filter(d -> d instanceof ImageDescriptor)
                .map(d -> (ImageDescriptor) d)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Desregistra um recurso pelo ID.
     *
     * @param id ID do recurso
     * @return true se removido, false se não existia
     */
    public boolean unregister(String id) {
        ResourceDescriptor removed = descriptors.remove(id);
        if (removed != null) {
            unregistrationCount.incrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Retorna o número total de recursos registrados.
     *
     * @return número de recursos
     */
    public int count() {
        return descriptors.size();
    }

    /**
     * Verifica se o registry está vazio.
     *
     * @return true se não há recursos
     */
    public boolean isEmpty() {
        return descriptors.isEmpty();
    }

    /**
     * Limpa todos os recursos do registry.
     */
    public void clear() {
        descriptors.clear();
    }

    /**
     * Retorna estatísticas de recursos por tipo.
     *
     * @return mapa de ResourceType → quantidade
     */
    public Map<ResourceType, Long> getStatistics() {
        return descriptors.values().stream()
                .collect(Collectors.groupingBy(
                        ResourceDescriptor::getResourceType,
                        Collectors.counting()
                ));
    }

    /**
     * Retorna o número total de registros.
     *
     * @return número de registros
     */
    public long getRegistrationCount() {
        return registrationCount.get();
    }

    /**
     * Retorna o número total de desregistros.
     *
     * @return número de desregistros
     */
    public long getUnregistrationCount() {
        return unregistrationCount.get();
    }

    /**
     * Retorna o número total de sobrescritas.
     *
     * @return número de sobrescritas
     */
    public long getOverwriteCount() {
        return overwriteCount.get();
    }

    /**
     * Retorna todos os IDs de recursos registrados.
     *
     * @return conjunto imutável de IDs
     */
    public Set<String> getAllIds() {
        return Collections.unmodifiableSet(descriptors.keySet());
    }
}