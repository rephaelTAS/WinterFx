package com.ossobo.winterfx.di.lifecycle;

import com.ossobo.winterfx.di.exceptions.LifecycleException;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.di.scopes.implementations.SingletonScope;
import com.ossobo.winterfx.di.scopes.implementations.ThreadScope;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * LifecycleManager v2.0
 *
 * Responsabilidade única: gerenciar o ciclo de vida dos beans.
 *
 * - Invocar @PostConstruct após criação + injeção
 * - Invocar @PreDestroy antes da destruição
 * - Publicar eventos via LifecycleEventPublisher
 * - Destruir escopos no shutdown
 *
 * Dependências injetadas: ReflectionCache, ReflectionProcessor,
 * ScopeManager, LifecycleEventPublisher.
 *
 * @since 2.0
 */
public final class LifecycleManager {

    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final ScopeManager scopeManager;
    private final LifecycleEventPublisher eventPublisher;

    public LifecycleManager(ReflectionCache reflectionCache,
                            ReflectionProcessor reflectionProcessor,
                            ScopeManager scopeManager,
                            LifecycleEventPublisher eventPublisher) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.scopeManager = scopeManager;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Inicializa o gerenciador. Chamado pelo DiContainer após a construção.
     */
    public void initialize() {
        eventPublisher.publishEvent(null, null,
                DependencyLifecycleListener.LifecycleEventType.CONTAINER_INITIALIZED);
    }

    // ===== @PostConstruct =====

    /**
     * Invoca métodos @PostConstruct numa instância recém-criada e injetada.
     *
     * @param instance instância do bean
     */
    public void invokePostConstruct(Object instance) {
        if (instance == null) return;

        Class<?> type = instance.getClass();
        List<Method> methods = reflectionCache.getPostConstructMethods(type);

        for (Method method : methods) {
            if (method.getParameterCount() != 0) {
                throw new LifecycleException(
                        "@PostConstruct deve ser sem argumentos: " + type.getName());
            }

            try {
                reflectionProcessor.invokeMethod(instance, method);
            } catch (Exception e) {
                eventPublisher.publishEvent(type, null,
                        DependencyLifecycleListener.LifecycleEventType.LIFECYCLE_ERROR,
                        instance, e);
                throw new LifecycleException(
                        "Erro no @PostConstruct: " + type.getName(), e);
            }
        }

        eventPublisher.publishEvent(type, null,
                DependencyLifecycleListener.LifecycleEventType.AFTER_POST_CONSTRUCT,
                instance);
    }

    // ===== @PreDestroy =====

    /**
     * Invoca métodos @PreDestroy numa instância.
     * Não lança exceção — continua a destruição dos outros beans.
     *
     * @param instance instância do bean
     */
    public void invokePreDestroy(Object instance) {
        if (instance == null) return;

        Class<?> type = instance.getClass();
        List<Method> methods = reflectionCache.getPreDestroyMethods(type);

        for (Method method : methods) {
            if (method.getParameterCount() != 0) {
                continue;
            }

            try {
                reflectionProcessor.invokeMethod(instance, method);
            } catch (Exception e) {
                eventPublisher.publishEvent(type, null,
                        DependencyLifecycleListener.LifecycleEventType.LIFECYCLE_ERROR,
                        instance, e);
            }
        }

        eventPublisher.publishEvent(type, null,
                DependencyLifecycleListener.LifecycleEventType.AFTER_PRE_DESTROY,
                instance);
    }

    // ===== LISTENERS (DELEGA PARA O PUBLISHER) =====

    public void addListener(DependencyLifecycleListener listener) {
        eventPublisher.registerListener(listener);
    }

    public void removeListener(DependencyLifecycleListener listener) {
        eventPublisher.unregisterListener(listener);
    }

    // ===== NOTIFICAÇÃO DE REGISTO =====

    public void notifyBeanRegistered(Class<?> type, String name) {
        eventPublisher.publishEvent(type, name,
                DependencyLifecycleListener.LifecycleEventType.BEAN_REGISTERED,
                type, name);
    }

    public void notifyBeanUnregistered(Class<?> type, String name) {
        eventPublisher.publishEvent(type, name,
                DependencyLifecycleListener.LifecycleEventType.BEAN_UNREGISTERED,
                type, name);
    }

    // ===== SHUTDOWN =====

    /**
     * Encerra o container:
     * 1. Destrói singletons (@PreDestroy + clear)
     * 2. Limpa ThreadLocals
     * 3. Notifica shutdown
     */
    public void shutdown() {
        // 1. Destruir Singletons
        SingletonScope singletonScope = scopeManager.getSingletonScope();
        if (singletonScope != null) {
            Map<Class<?>, Object> singletons = singletonScope.getAllInstances();
            singletons.values().forEach(instance -> {
                eventPublisher.publishEvent(instance.getClass(), null,
                        DependencyLifecycleListener.LifecycleEventType.BEFORE_DESTRUCTION,
                        instance);
                invokePreDestroy(instance);
            });
            singletonScope.destroy();
            eventPublisher.publishEvent(null, null,
                    DependencyLifecycleListener.LifecycleEventType.SCOPE_DESTROYED,
                    "singleton");
        }

        // 2. Limpar ThreadScope
        ThreadScope threadScope = scopeManager.getThreadScope();
        if (threadScope != null) {
            threadScope.clearAllThreads();
        }

        // 3. Limpar escopos
        scopeManager.clear();

        // 4. Notificar
        eventPublisher.publishEvent(null, null,
                DependencyLifecycleListener.LifecycleEventType.CONTAINER_SHUTDOWN);
        eventPublisher.clearListeners();
    }

    /**
     * Destrói o escopo da thread atual.
     * Chamado pelo filtro de requisições HTTP.
     */
    public void destroyThreadScope() {
        ThreadScope threadScope = scopeManager.getThreadScope();
        if (threadScope == null) return;

        Map<Class<?>, Object> instances = threadScope.clearAndGetInstances();
        if (instances != null && !instances.isEmpty()) {
            instances.values().forEach(this::invokePreDestroy);
        }
    }

    /**
     * @deprecated Use shutdown().
     */
    @Deprecated
    public void destroy() {
        shutdown();
    }
}