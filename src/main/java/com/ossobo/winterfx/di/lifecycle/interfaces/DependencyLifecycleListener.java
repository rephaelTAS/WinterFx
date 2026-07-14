package com.ossobo.winterfx.di.lifecycle.interfaces;

import java.util.Collections;
import java.util.Set;

/**
 * Contrato para ouvintes de eventos do ciclo de vida de beans gerenciados pelo contêiner.
 *
 * <p>Permite que componentes externos observem e reajam a diversas fases do ciclo de vida,
 * como criação, injeção de dependências, destruição e erros.</p>
 *
 * <p>Os métodos desta interface possuem implementações padrão vazias, permitindo que as
 * classes de implementação substituam apenas os eventos de seu interesse.</p>
 */
public interface DependencyLifecycleListener {

    /**
     * Chamado imediatamente antes da criação de uma nova instância de bean.
     *
     * @param beanClass A classe do bean que será criada.
     */
    default void beforeBeanCreation(Class<?> beanClass) {}

    /**
     * Chamado imediatamente após a instância bruta do bean ser criada,
     * mas antes da injeção de suas dependências.
     *
     * @param beanInstance A instância recém-criada do bean.
     */
    default void afterBeanCreation(Object beanInstance) {}

    /**
     * Chamado após a conclusão da injeção de dependências no bean,
     * mas antes da execução dos métodos {@code @PostConstruct}.
     *
     * @param beanInstance A instância do bean com suas dependências já injetadas.
     */
    default void afterDependencyInjection(Object beanInstance) {}

    /**
     * Chamado após a execução bem-sucedida de todos os métodos {@code @PostConstruct}
     * do bean, indicando que a inicialização foi concluída.
     *
     * @param beanInstance A instância do bean totalmente inicializada.
     */
    default void afterPostConstruct(Object beanInstance) {}

    /**
     * Chamado imediatamente antes do contêiner iniciar o processo de destruição do bean.
     *
     * @param beanInstance A instância do bean que será destruída.
     */
    default void beforeBeanDestruction(Object beanInstance) {}

    /**
     * Chamado após a execução dos métodos {@code @PreDestroy} do bean.
     *
     * @param beanInstance A instância do bean após a finalização de sua destruição.
     */
    default void afterPreDestroy(Object beanInstance) {}

    /**
     * Chamado quando ocorre um erro genérico durante o processamento do ciclo de vida de um bean.
     *
     * @param beanInstance A instância do bean onde o erro ocorreu (pode ser nulo se o erro
     *                     aconteceu antes da instanciação).
     * @param error         A exceção que provocou o erro.
     */
    default void onLifecycleError(Object beanInstance, Throwable error) {}

    /**
     * Chamado quando o contêiner falha ao resolver uma dependência requerida por um bean.
     *
     * @param beanClass       A classe do bean que necessitava da dependência.
     * @param dependencyName  O nome ou identificador da dependência que não pôde ser resolvida.
     * @param error           A exceção detalhando a causa da falha na resolução.
     */
    default void onDependencyResolutionError(Class<?> beanClass, String dependencyName, Throwable error) {}

    /**
     * Chamado quando uma nova definição de bean é registrada no contêiner.
     *
     * @param beanClass A classe do bean registrado.
     * @param beanName  O nome lógico atribuído ao bean.
     */
    default void onBeanRegistered(Class<?> beanClass, String beanName) {}

    /**
     * Chamado quando uma definição de bean é removida do contêiner.
     *
     * @param beanClass A classe do bean removido.
     * @param beanName  O nome lógico do bean que foi removido.
     */
    default void onBeanUnregistered(Class<?> beanClass, String beanName) {}

    /**
     * Chamado quando o contêiner de injeção finaliza seu processo de inicialização completo.
     */
    default void onContainerInitialized() {}

    /**
     * Chamado quando o contêiner de injeção é desligado ou encerrado.
     */
    default void onContainerShutdown() {}

    /**
     * Chamado quando um novo escopo de beans é criado e ativado no contêiner.
     *
     * @param scopeName O identificador do escopo criado.
     */
    default void onScopeCreated(String scopeName) {}

    /**
     * Chamado quando um escopo de beans é destruído e encerrado.
     *
     * @param scopeName O identificador do escopo destruído.
     */
    default void onScopeDestroyed(String scopeName) {}

    /**
     * Retorna os tipos específicos de beans dos quais este ouvinte deseja receber eventos.
     *
     * <p>Retornar um conjunto vazio indica que o ouvinte tem interesse em eventos de
     * todos os tipos de beans.</p>
     *
     * @return Um conjunto imutável contendo as classes de interesse.
     */
    default Set<Class<?>> getInterestedBeanTypes() {
        return Collections.emptySet();
    }

    /**
     * Retorna os nomes lógicos específicos de beans dos quais este ouvinte deseja receber eventos.
     *
     * <p>Retornar um conjunto vazio indica que o ouvinte tem interesse em eventos de
     * todos os beans, independentemente do nome.</p>
     *
     * @return Um conjunto imutável contendo os nomes de beans de interesse.
     */
    default Set<String> getInterestedBeanNames() {
        return Collections.emptySet();
    }

    /**
     * Retorna os tipos específicos de eventos que este ouvinte deseja processar.
     *
     * <p>Retornar um conjunto vazio indica que o ouvinte deseja receber notificações
     * de todos os tipos de eventos disponíveis.</p>
     *
     * @return Um conjunto imutável contendo os tipos de eventos de interesse.
     */
    default Set<LifecycleEventType> getInterestedEvents() {
        return Collections.emptySet();
    }

    /**
     * Método de filtragem central para determinar se este ouvinte deve ser notificado
     * com base no contexto do evento.
     *
     * <p>Retorna {@code true} se o ouvinte não declarou filtros restritivos para a classe,
     * nome ou tipo de evento fornecidos, ou se o evento corresponde aos filtros declarados.</p>
     *
     * @param beanClass A classe do bean relacionado ao evento.
     * @param beanName  O nome do bean relacionado ao evento.
     * @param eventType O tipo do evento disparado.
     * @return {@code true} se o ouvinte está interessado e deve ser notificado, {@code false} caso contrário.
     */
    default boolean isInterestedInEvent(Class<?> beanClass, String beanName, LifecycleEventType eventType) {
        Set<Class<?>> interestedTypes = getInterestedBeanTypes();
        if (!interestedTypes.isEmpty() && !interestedTypes.contains(beanClass)) {
            return false;
        }

        Set<String> interestedNames = getInterestedBeanNames();
        if (!interestedNames.isEmpty() && !interestedNames.contains(beanName)) {
            return false;
        }

        Set<LifecycleEventType> interestedEvents = getInterestedEvents();
        if (!interestedEvents.isEmpty() && !interestedEvents.contains(eventType)) {
            return false;
        }

        return true;
    }

    /**
     * Enumeração de todos os tipos de eventos disparados durante o ciclo de vida
     * do contêiner e de seus beans gerenciados.
     */
    enum LifecycleEventType {
        /** Disparado antes da instanciação do bean. */
        BEFORE_CREATION,
        /** Disparado após a instanciação bruta do bean. */
        AFTER_CREATION,
        /** Disparado após a conclusão da injeção de dependências. */
        AFTER_INJECTION,
        /** Disparado após a execução dos métodos de pós-construção. */
        AFTER_POST_CONSTRUCT,
        /** Disparado antes do início da destruição do bean. */
        BEFORE_DESTRUCTION,
        /** Disparado após a execução dos métodos de pré-destruição. */
        AFTER_PRE_DESTROY,
        /** Disparado quando uma exceção ocorre durante o ciclo de vida do bean. */
        LIFECYCLE_ERROR,
        /** Disparado quando falha a resolução de uma dependência do bean. */
        DEPENDENCY_ERROR,
        /** Disparado quando um bean é registrado nos metadados do contêiner. */
        BEAN_REGISTERED,
        /** Disparado quando um bean é removido dos metadados do contêiner. */
        BEAN_UNREGISTERED,
        /** Disparado quando o contêiner termina sua fase de bootstrap. */
        CONTAINER_INITIALIZED,
        /** Disparado quando o contêiner é encerrado. */
        CONTAINER_SHUTDOWN,
        /** Disparado na ativação de um novo escopo de beans. */
        SCOPE_CREATED,
        /** Disparado na desativação e limpeza de um escopo de beans. */
        SCOPE_DESTROYED
    }
}