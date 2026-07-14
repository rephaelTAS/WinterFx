package com.ossobo.winterfx.di.lifecycle.listener;

import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener.LifecycleEventType;

import java.util.Set;

/**
 * Implementação base (silenciosa) de um ouvinte de ciclo de vida.
 *
 * <p>Esta classe funciona como um ponto de partida ou como um observador que consome
 * eventos sem executar nenhuma lógica. Pode ser estendida para implementar
 * comportamentos específicos apenas para os eventos de interesse, aproveitando
 * os métodos padrão vazios da interface.</p>
 */
public class LifecycleLogger implements DependencyLifecycleListener {

    @Override
    public void beforeBeanCreation(Class<?> beanClass) {
    }

    @Override
    public void afterPostConstruct(Object beanInstance) {
    }

    @Override
    public void onLifecycleError(Object beanInstance, Throwable error) {
    }

    @Override
    public void onContainerInitialized() {
    }

    @Override
    public void onContainerShutdown() {
    }

    @Override
    public Set<LifecycleEventType> getInterestedEvents() {
        return Set.of(
                LifecycleEventType.BEFORE_CREATION,
                LifecycleEventType.AFTER_POST_CONSTRUCT,
                LifecycleEventType.LIFECYCLE_ERROR,
                LifecycleEventType.CONTAINER_INITIALIZED,
                LifecycleEventType.CONTAINER_SHUTDOWN
        );
    }
}