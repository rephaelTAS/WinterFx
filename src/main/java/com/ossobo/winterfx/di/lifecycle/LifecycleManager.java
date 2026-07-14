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
 * Gerenciador responsável por orquestrar a execução do ciclo de vida dos beans.
 *
 * <p>Esta classe coordena a invocação de métodos anotados com {@code @PostConstruct}
 * e {@code @PreDestroy}, gerencia o processo de desligamento (shutdown) dos escopos
 * do contêiner e atua como fachada para o registro de ouvintes de ciclo de vida
 * ({@link DependencyLifecycleListener}), delegando a publicação real dos eventos
 * para o {@link LifecycleEventPublisher}.</p>
 *
 * @since 2.0
 */
public final class LifecycleManager {

    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final ScopeManager scopeManager;
    private final LifecycleEventPublisher eventPublisher;

    /**
     * Constrói o gerenciador de ciclo de vida com as dependências necessárias
     * para reflexão, gerenciamento de escopos e publicação de eventos.
     *
     * @param reflectionCache    Cache para otimização da busca por métodos de ciclo de vida.
     * @param reflectionProcessor Utilitário para invocação segura de métodos via reflexão.
     * @param scopeManager       Gerenciador de escopos do contêiner.
     * @param eventPublisher     Publicador de eventos de ciclo de vida.
     */
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
     * Inicializa o gerenciador, publicando o evento de que o contêiner foi totalmente inicializado.
     */
    public void initialize() {
        eventPublisher.publishEvent(null, null,
                DependencyLifecycleListener.LifecycleEventType.CONTAINER_INITIALIZED);
    }

    // ===== @PostConstruct =====

    /**
     * Identifica e invoca todos os métodos anotados com {@code @PostConstruct}
     * na instância do bean fornecida.
     *
     * @param instance A instância do bean a ser inicializada.
     * @throws LifecycleException Se um método anotado possuir parâmetros ou se a
     *                            invocação falhar, interrompendo o ciclo de vida do bean.
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
     * Identifica e invoca todos os métodos anotados com {@code @PreDestroy}
     * na instância do bean fornecida.
     *
     * <p>Este método não propaga exceções. Se um método {@code @PreDestroy} falhar,
     * o erro é capturado e publicado como evento, mas o processo de destruição
     * continua para os demais beans, garantindo a máxima limpeza possível.</p>
     *
     * @param instance A instância do bean a ser destruída.
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

    /**
     * Registra um ouvinte de ciclo de vida no publicador de eventos interno.
     *
     * @param listener O ouvinte a ser registrado.
     */
    public void addListener(DependencyLifecycleListener listener) {
        eventPublisher.registerListener(listener);
    }

    /**
     * Remove um ouvinte de ciclo de vida do publicador de eventos interno.
     *
     * @param listener O ouvinte a ser removido.
     */
    public void removeListener(DependencyLifecycleListener listener) {
        eventPublisher.unregisterListener(listener);
    }

    // ===== NOTIFICAÇÃO DE REGISTRO =====

    /**
     * Notifica o contêiner sobre o registro de um novo bean.
     *
     * @param type A classe do bean registrado.
     * @param name O nome lógico do bean registrado.
     */
    public void notifyBeanRegistered(Class<?> type, String name) {
        eventPublisher.publishEvent(type, name,
                DependencyLifecycleListener.LifecycleEventType.BEAN_REGISTERED,
                type, name);
    }

    /**
     * Notifica o contêiner sobre a remoção de um bean.
     *
     * @param type A classe do bean removido.
     * @param name O nome lógico do bean removido.
     */
    public void notifyBeanUnregistered(Class<?> type, String name) {
        eventPublisher.publishEvent(type, name,
                DependencyLifecycleListener.LifecycleEventType.BEAN_UNREGISTERED,
                type, name);
    }

    // ===== SHUTDOWN =====

    /**
     * Executa o procedimento de encerramento ordenado do contêiner.
     *
     * <p>O processo segue a seguinte ordem:</p>
     * <ol>
     *     <li>Publica evento de destruição e invoca {@code @PreDestroy} em todos os beans Singleton.</li>
     *     <li>Destrói o escopo Singleton.</li>
     *     <li>Limpa as instâncias associadas às threads (ThreadScope).</li>
     *     <li>Limpa todos os escopos do gerenciador.</li>
     *     <li>Publica o evento de desligamento do contêiner e remove todos os ouvintes.</li>
     * </ol>
     */
    public void shutdown() {
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

        ThreadScope threadScope = scopeManager.getThreadScope();
        if (threadScope != null) {
            threadScope.clearAllThreads();
        }

        scopeManager.clear();

        eventPublisher.publishEvent(null, null,
                DependencyLifecycleListener.LifecycleEventType.CONTAINER_SHUTDOWN);
        eventPublisher.clearListeners();
    }

    /**
     * Destrói todas as instâncias de beans associadas à thread atual no escopo de thread.
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
     * @deprecated Utilize {@link #shutdown()} para uma terminação mais descritiva e completa.
     */
    @Deprecated
    public void destroy() {
        shutdown();
    }
}