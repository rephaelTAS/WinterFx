package com.ossobo.winterfx.di.lifecycle.events;

import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Publicador central de eventos do ciclo de vida de beans do contêiner de injeção.
 *
 * <p>Esta classe gerencia os ouvintes ({@link DependencyLifecycleListener}) registrados
 * e distribui os eventos de ciclo de vida de forma eficiente, suportando filtragem
 * por tipo de bean, nome do bean e tipo de evento específico.</p>
 *
 * <p>A publicação de eventos pode ser desabilitada globalmente através do método
 * {@link #setEnabled(boolean)}, útil para cenários onde a sobrecarga de notificação
 * não é desejada.</p>
 */
public class LifecycleEventPublisher {
    private final Set<DependencyLifecycleListener> globalListeners;
    private final Map<Class<?>, Set<DependencyLifecycleListener>> typeSpecificListeners;
    private final Map<String, Set<DependencyLifecycleListener>> nameSpecificListeners;
    private final Map<DependencyLifecycleListener.LifecycleEventType, Set<DependencyLifecycleListener>> eventSpecificListeners;

    private boolean enabled = true;

    /**
     * Construtor que inicializa as estruturas de armazenamento dos ouvintes
     * e pré-registra os conjuntos para todos os tipos de evento disponíveis.
     */
    public LifecycleEventPublisher() {
        this.globalListeners = new CopyOnWriteArraySet<>();
        this.typeSpecificListeners = new ConcurrentHashMap<>();
        this.nameSpecificListeners = new ConcurrentHashMap<>();
        this.eventSpecificListeners = new ConcurrentHashMap<>();

        for (DependencyLifecycleListener.LifecycleEventType eventType :
                DependencyLifecycleListener.LifecycleEventType.values()) {
            eventSpecificListeners.put(eventType, new CopyOnWriteArraySet<>());
        }
    }

    /**
     * Registra um ouvinte para receber notificações de eventos do ciclo de vida.
     *
     * <p>O ouvinte é automaticamente indexado com base nos tipos de beans, nomes
     * e tipos de eventos que declarar interesse.</p>
     *
     * @param listener O ouvinte a ser registrado.
     * @throws NullPointerException se o ouvinte fornecido for nulo.
     */
    public void registerListener(DependencyLifecycleListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");

        globalListeners.add(listener);

        for (Class<?> beanType : listener.getInterestedBeanTypes()) {
            typeSpecificListeners
                    .computeIfAbsent(beanType, k -> new CopyOnWriteArraySet<>())
                    .add(listener);
        }

        for (String beanName : listener.getInterestedBeanNames()) {
            nameSpecificListeners
                    .computeIfAbsent(beanName, k -> new CopyOnWriteArraySet<>())
                    .add(listener);
        }

        for (DependencyLifecycleListener.LifecycleEventType eventType : listener.getInterestedEvents()) {
            eventSpecificListeners
                    .computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>())
                    .add(listener);
        }
    }

    /**
     * Remove um ouvinte previamente registrado de todas as listas de notificação.
     *
     * @param listener O ouvinte a ser removido.
     * @throws NullPointerException se o ouvinte fornecido for nulo.
     */
    public void unregisterListener(DependencyLifecycleListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");

        globalListeners.remove(listener);

        for (Set<DependencyLifecycleListener> listeners : typeSpecificListeners.values()) {
            listeners.remove(listener);
        }

        for (Set<DependencyLifecycleListener> listeners : nameSpecificListeners.values()) {
            listeners.remove(listener);
        }

        for (Set<DependencyLifecycleListener> listeners : eventSpecificListeners.values()) {
            listeners.remove(listener);
        }
    }

    /**
     * Remove todos os ouvintes registrados, independentemente de seu tipo de filtro.
     */
    public void clearListeners() {
        globalListeners.clear();
        typeSpecificListeners.clear();
        nameSpecificListeners.clear();
        for (Set<DependencyLifecycleListener> listeners : eventSpecificListeners.values()) {
            listeners.clear();
        }
    }

    /**
     * Publica um evento para todos os ouvintes registrados que demonstrarem interesse.
     *
     * <p>A notificação ocorre respeitando a seguinte prioridade de verificação:</p>
     * <ol>
     *     <li>Ouvintes globais</li>
     *     <li>Ouvintes específicos por tipo de bean</li>
     *     <li>Ouvintes específicos por nome de bean</li>
     *     <li>Ouvintes específicos por tipo de evento</li>
     * </ol>
     * <p>Um mesmo ouvinte não será notificado mais de uma vez para o mesmo evento,
     * mesmo que se encaixe em múltiplas categorias.</p>
     *
     * @param beanClass A classe do bean relacionado ao evento (pode ser nulo).
     * @param beanName  O nome do bean relacionado ao evento (pode ser nulo).
     * @param eventType O tipo específico do evento do ciclo de vida.
     * @param eventData  Dados adicionais variáveis relevantes para o evento.
     */
    public void publishEvent(Class<?> beanClass, String beanName,
                             DependencyLifecycleListener.LifecycleEventType eventType,
                             Object... eventData) {
        if (!enabled) {
            return;
        }

        Set<DependencyLifecycleListener> notifiedListeners = new HashSet<>();

        for (DependencyLifecycleListener listener : globalListeners) {
            if (listener.isInterestedInEvent(beanClass, beanName, eventType)) {
                notifyListener(listener, eventType, eventData);
                notifiedListeners.add(listener);
            }
        }

        if (beanClass != null) {
            Set<DependencyLifecycleListener> typeListeners = typeSpecificListeners.get(beanClass);
            if (typeListeners != null) {
                for (DependencyLifecycleListener listener : typeListeners) {
                    if (!notifiedListeners.contains(listener) &&
                            listener.isInterestedInEvent(beanClass, beanName, eventType)) {
                        notifyListener(listener, eventType, eventData);
                        notifiedListeners.add(listener);
                    }
                }
            }
        }

        if (beanName != null) {
            Set<DependencyLifecycleListener> nameListeners = nameSpecificListeners.get(beanName);
            if (nameListeners != null) {
                for (DependencyLifecycleListener listener : nameListeners) {
                    if (!notifiedListeners.contains(listener) &&
                            listener.isInterestedInEvent(beanClass, beanName, eventType)) {
                        notifyListener(listener, eventType, eventData);
                        notifiedListeners.add(listener);
                    }
                }
            }
        }

        Set<DependencyLifecycleListener> eventListeners = eventSpecificListeners.get(eventType);
        if (eventListeners != null) {
            for (DependencyLifecycleListener listener : eventListeners) {
                if (!notifiedListeners.contains(listener) &&
                        listener.isInterestedInEvent(beanClass, beanName, eventType)) {
                    notifyListener(listener, eventType, eventData);
                    notifiedListeners.add(listener);
                }
            }
        }
    }

    /**
     * Invoca o método de callback apropriado no ouvinte com base no tipo de evento.
     *
     * <p>Exceções lançadas durante a notificação de um ouvinte são absorvidas silenciosamente
     * para evitar que um único ouvinte com erro interrompa a notificação dos demais.</p>
     *
     * @param listener   O ouvinte a ser notificado.
     * @param eventType  O tipo do evento.
     * @param eventData  Os dados do evento.
     */
    private void notifyListener(DependencyLifecycleListener listener,
                                DependencyLifecycleListener.LifecycleEventType eventType,
                                Object[] eventData) {
        try {
            switch (eventType) {
                case BEFORE_CREATION:
                    if (eventData.length >= 1 && eventData[0] instanceof Class) {
                        listener.beforeBeanCreation((Class<?>) eventData[0]);
                    }
                    break;

                case AFTER_CREATION:
                    if (eventData.length >= 1) {
                        listener.afterBeanCreation(eventData[0]);
                    }
                    break;

                case AFTER_INJECTION:
                    if (eventData.length >= 1) {
                        listener.afterDependencyInjection(eventData[0]);
                    }
                    break;

                case AFTER_POST_CONSTRUCT:
                    if (eventData.length >= 1) {
                        listener.afterPostConstruct(eventData[0]);
                    }
                    break;

                case BEFORE_DESTRUCTION:
                    if (eventData.length >= 1) {
                        listener.beforeBeanDestruction(eventData[0]);
                    }
                    break;

                case AFTER_PRE_DESTROY:
                    if (eventData.length >= 1) {
                        listener.afterPreDestroy(eventData[0]);
                    }
                    break;

                case LIFECYCLE_ERROR:
                    if (eventData.length >= 2 && eventData[1] instanceof Throwable) {
                        listener.onLifecycleError(eventData[0], (Throwable) eventData[1]);
                    }
                    break;

                case DEPENDENCY_ERROR:
                    if (eventData.length >= 3 && eventData[0] instanceof Class &&
                            eventData[2] instanceof Throwable) {
                        listener.onDependencyResolutionError(
                                (Class<?>) eventData[0], (String) eventData[1], (Throwable) eventData[2]);
                    }
                    break;

                case BEAN_REGISTERED:
                    if (eventData.length >= 2 && eventData[0] instanceof Class) {
                        listener.onBeanRegistered((Class<?>) eventData[0], (String) eventData[1]);
                    }
                    break;

                case BEAN_UNREGISTERED:
                    if (eventData.length >= 2 && eventData[0] instanceof Class) {
                        listener.onBeanUnregistered((Class<?>) eventData[0], (String) eventData[1]);
                    }
                    break;

                case CONTAINER_INITIALIZED:
                    listener.onContainerInitialized();
                    break;

                case CONTAINER_SHUTDOWN:
                    listener.onContainerShutdown();
                    break;

                case SCOPE_CREATED:
                    if (eventData.length >= 1 && eventData[0] instanceof String) {
                        listener.onScopeCreated((String) eventData[0]);
                    }
                    break;

                case SCOPE_DESTROYED:
                    if (eventData.length >= 1 && eventData[0] instanceof String) {
                        listener.onScopeDestroyed((String) eventData[0]);
                    }
                    break;
            }
        } catch (Exception e) {
        }
    }

    /**
     * Habilita ou desabilita a publicação de eventos globalmente.
     *
     * @param enabled {@code true} para habilitar, {@code false} para desabilitar.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Verifica se a publicação de eventos está atualmente habilitada.
     *
     * @return {@code true} se habilitada, {@code false} caso contrário.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Retorna o número total de ouvintes globais registrados.
     *
     * @return A quantidade de ouvintes globais.
     */
    public int getListenerCount() {
        return globalListeners.size();
    }

    /**
     * Retorna um mapa contendo estatísticas sobre a quantidade de ouvintes
     * registrados em cada categoria de filtro.
     *
     * @return Um mapa imutável com as chaves representando as categorias e os valores as quantidades.
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();

        stats.put("globalListeners", globalListeners.size());
        stats.put("typeSpecificListeners", typeSpecificListeners.size());
        stats.put("nameSpecificListeners", nameSpecificListeners.size());

        int totalEventListeners = 0;
        for (Set<DependencyLifecycleListener> listeners : eventSpecificListeners.values()) {
            totalEventListeners += listeners.size();
        }
        stats.put("eventSpecificListeners", totalEventListeners);

        return stats;
    }
}