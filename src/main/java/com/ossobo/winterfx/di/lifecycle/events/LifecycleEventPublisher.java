package com.ossobo.winterfx.di.lifecycle.events;


import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Publicador de eventos do ciclo de vida de dependências.
 * Gerencia os listeners e distribui os eventos apropriadamente.
 */
public class LifecycleEventPublisher {
    private final Set<DependencyLifecycleListener> globalListeners;
    private final Map<Class<?>, Set<DependencyLifecycleListener>> typeSpecificListeners;
    private final Map<String, Set<DependencyLifecycleListener>> nameSpecificListeners;
    private final Map<DependencyLifecycleListener.LifecycleEventType, Set<DependencyLifecycleListener>> eventSpecificListeners;

    private boolean enabled = true;

    public LifecycleEventPublisher() {
        this.globalListeners = new CopyOnWriteArraySet<>();
        this.typeSpecificListeners = new ConcurrentHashMap<>();
        this.nameSpecificListeners = new ConcurrentHashMap<>();
        this.eventSpecificListeners = new ConcurrentHashMap<>();

        // Inicializa os mapas para todos os tipos de evento
        for (DependencyLifecycleListener.LifecycleEventType eventType :
                DependencyLifecycleListener.LifecycleEventType.values()) {
            eventSpecificListeners.put(eventType, new CopyOnWriteArraySet<>());
        }
    }

    /**
     * Registra um listener para eventos do ciclo de vida
     * @param listener O listener a ser registrado
     */
    public void registerListener(DependencyLifecycleListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");

        globalListeners.add(listener);

        // Registra para tipos específicos
        for (Class<?> beanType : listener.getInterestedBeanTypes()) {
            typeSpecificListeners
                    .computeIfAbsent(beanType, k -> new CopyOnWriteArraySet<>())
                    .add(listener);
        }

        // Registra para nomes específicos
        for (String beanName : listener.getInterestedBeanNames()) {
            nameSpecificListeners
                    .computeIfAbsent(beanName, k -> new CopyOnWriteArraySet<>())
                    .add(listener);
        }

        // Registra para eventos específicos
        for (DependencyLifecycleListener.LifecycleEventType eventType : listener.getInterestedEvents()) {
            eventSpecificListeners
                    .computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>())
                    .add(listener);
        }
    }

    /**
     * Remove um listener registrado
     * @param listener O listener a ser removido
     */
    public void unregisterListener(DependencyLifecycleListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");

        globalListeners.remove(listener);

        // Remove de tipos específicos
        for (Set<DependencyLifecycleListener> listeners : typeSpecificListeners.values()) {
            listeners.remove(listener);
        }

        // Remove de nomes específicos
        for (Set<DependencyLifecycleListener> listeners : nameSpecificListeners.values()) {
            listeners.remove(listener);
        }

        // Remove de eventos específicos
        for (Set<DependencyLifecycleListener> listeners : eventSpecificListeners.values()) {
            listeners.remove(listener);
        }
    }

    /**
     * Remove todos os listeners
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
     * Publica um evento para todos os listeners interessados
     * @param beanClass A classe do bean relacionado ao evento (pode ser null para eventos de container)
     * @param beanName O nome do bean relacionado ao evento (pode ser null)
     * @param eventType O tipo de evento
     * @param eventData Dados adicionais do evento
     */
    public void publishEvent(Class<?> beanClass, String beanName,
                             DependencyLifecycleListener.LifecycleEventType eventType,
                             Object... eventData) {
        if (!enabled) {
            return;
        }

        Set<DependencyLifecycleListener> notifiedListeners = new HashSet<>();

        // Notifica listeners globais
        for (DependencyLifecycleListener listener : globalListeners) {
            if (listener.isInterestedInEvent(beanClass, beanName, eventType)) {
                notifyListener(listener, eventType, eventData);
                notifiedListeners.add(listener);
            }
        }

        // Notifica listeners específicos por tipo
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

        // Notifica listeners específicos por nome
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

        // Notifica listeners específicos por evento
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
     * Notifica um listener específico sobre um evento
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
     * Habilita ou desabilita a publicação de eventos
     * @param enabled true para habilitar, false para desabilitar
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Verifica se a publicação de eventos está habilitada
     * @return true se a publicação de eventos está habilitada
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Obtém o número de listeners registrados
     * @return O número total de listeners
     */
    public int getListenerCount() {
        return globalListeners.size();
    }

    /**
     * Obtém estatísticas de listeners
     * @return Mapa com estatísticas de listeners
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