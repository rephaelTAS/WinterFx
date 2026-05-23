package com.ossobo.winterfx.di.lifecycle.interfaces;

import java.util.Collections;
import java.util.Set;

/**
 * Interface para listeners de eventos do ciclo de vida de dependências.
 * Permite que componentes externos recebam notificações sobre eventos
 * do ciclo de vida dos beans gerenciados pelo container.
 */
public interface DependencyLifecycleListener {

    /**
     * Chamado quando um bean está prestes a ser criado
     * @param beanClass A classe do bean que será criado
     */
    default void beforeBeanCreation(Class<?> beanClass) {}

    /**
     * Chamado quando um bean foi criado mas antes da injeção de dependências
     * @param beanInstance A instância do bean recém-criada
     */
    default void afterBeanCreation(Object beanInstance) {}

    /**
     * Chamado após a injeção de dependências mas antes dos callbacks @PostConstruct
     * @param beanInstance A instância do bean com dependências injetadas
     */
    default void afterDependencyInjection(Object beanInstance) {}

    /**
     * Chamado após a execução dos métodos @PostConstruct
     * @param beanInstance A instância do bean totalmente inicializada
     */
    default void afterPostConstruct(Object beanInstance) {}

    /**
     * Chamado quando um bean está prestes a ser destruído
     * @param beanInstance A instância do bean que será destruída
     */
    default void beforeBeanDestruction(Object beanInstance) {}

    /**
     * Chamado após a execução dos métodos @PreDestroy
     * @param beanInstance A instância do bean após destruição
     */
    default void afterPreDestroy(Object beanInstance) {}

    /**
     * Chamado quando ocorre um erro durante o ciclo de vida de um bean
     * @param beanInstance A instância do bean onde ocorreu o erro (pode ser null)
     * @param error O erro que ocorreu
     */
    default void onLifecycleError(Object beanInstance, Throwable error) {}

    /**
     * Chamado quando uma dependência não pode ser resolvida
     * @param beanClass A classe do bean que precisa da dependência
     * @param dependencyName O nome da dependência que não pôde ser resolvida
     * @param error O erro que ocorreu durante a resolução
     */
    default void onDependencyResolutionError(Class<?> beanClass, String dependencyName, Throwable error) {}

    /**
     * Chamado quando um bean é registrado no container
     * @param beanClass A classe do bean registrado
     * @param beanName O nome do bean registrado
     */
    default void onBeanRegistered(Class<?> beanClass, String beanName) {}

    /**
     * Chamado quando um bean é removido do container
     * @param beanClass A classe do bean removido
     * @param beanName O nome do bean removido
     */
    default void onBeanUnregistered(Class<?> beanClass, String beanName) {}

    /**
     * Chamado quando o container é inicializado
     */
    default void onContainerInitialized() {}

    /**
     * Chamado quando o container é desligado
     */
    default void onContainerShutdown() {}

    /**
     * Chamado quando um scope é criado
     * @param scopeName O nome do scope criado
     */
    default void onScopeCreated(String scopeName) {}

    /**
     * Chamado quando um scope é destruído
     * @param scopeName O nome do scope destruído
     */
    default void onScopeDestroyed(String scopeName) {}

    /**
     * Retorna os tipos de beans que este listener está interessado
     * Retorna empty set para receber eventos de todos os tipos
     * @return Conjunto de classes de interesse
     */
    default Set<Class<?>> getInterestedBeanTypes() {
        return Collections.emptySet();
    }

    /**
     * Retorna os nomes de beans específicos que este listener está interessado
     * Retorna empty set para receber eventos de todos os beans
     * @return Conjunto de nomes de beans de interesse
     */
    default Set<String> getInterestedBeanNames() {
        return Collections.emptySet();
    }

    /**
     * Retorna os eventos que este listener deseja receber
     * Retorna empty set para receber todos os eventos
     * @return Conjunto de tipos de evento de interesse
     */
    default Set<LifecycleEventType> getInterestedEvents() {
        return Collections.emptySet();
    }

    /**
     * Verifica se este listener está interessado em um evento específico
     * @param beanClass A classe do bean relacionado ao evento
     * @param beanName O nome do bean relacionado ao evento
     * @param eventType O tipo de evento
     * @return true se o listener está interessado no evento
     */
    default boolean isInterestedInEvent(Class<?> beanClass, String beanName, LifecycleEventType eventType) {
        // Verifica se está interessado no tipo de bean
        Set<Class<?>> interestedTypes = getInterestedBeanTypes();
        if (!interestedTypes.isEmpty() && !interestedTypes.contains(beanClass)) {
            return false;
        }

        // Verifica se está interessado no nome do bean
        Set<String> interestedNames = getInterestedBeanNames();
        if (!interestedNames.isEmpty() && !interestedNames.contains(beanName)) {
            return false;
        }

        // Verifica se está interessado no tipo de evento
        Set<LifecycleEventType> interestedEvents = getInterestedEvents();
        if (!interestedEvents.isEmpty() && !interestedEvents.contains(eventType)) {
            return false;
        }

        return true;
    }

    /**
     * Tipos de eventos do ciclo de vida
     */
    enum LifecycleEventType {
        /** Antes da criação de um bean */
        BEFORE_CREATION,
        /** Após a criação de um bean */
        AFTER_CREATION,
        /** Após a injeção de dependências */
        AFTER_INJECTION,
        /** Após a execução de @PostConstruct */
        AFTER_POST_CONSTRUCT,
        /** Antes da destruição de um bean */
        BEFORE_DESTRUCTION,
        /** Após a execução de @PreDestroy */
        AFTER_PRE_DESTROY,
        /** Erro no ciclo de vida */
        LIFECYCLE_ERROR,
        /** Erro na resolução de dependência */
        DEPENDENCY_ERROR,
        /** Bean registrado no container */
        BEAN_REGISTERED,
        /** Bean removido do container */
        BEAN_UNREGISTERED,
        /** Container inicializado */
        CONTAINER_INITIALIZED,
        /** Container desligado */
        CONTAINER_SHUTDOWN,
        /** Scope criado */
        SCOPE_CREATED,
        /** Scope destruído */
        SCOPE_DESTROYED
    }
}