package com.ossobo.winterfx.di.lifecycle.listener;

import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;

import java.util.Set;

/**
 * Exemplo de listener para logging do ciclo de vida
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