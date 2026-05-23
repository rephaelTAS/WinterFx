package com.ossobo.winterfx.di.lifecycle.listener;

import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;

import java.util.Set;

/**
 * Exemplo de listener para logging do ciclo de vida
 */
public class LifecycleLogger implements DependencyLifecycleListener {

    @Override
    public void beforeBeanCreation(Class<?> beanClass) {
        System.out.println("[LIFECYCLE] Creating bean: " + beanClass.getSimpleName());
    }

    @Override
    public void afterPostConstruct(Object beanInstance) {
        System.out.println("[LIFECYCLE] Bean initialized: " +
                beanInstance.getClass().getSimpleName());
    }

    @Override
    public void onLifecycleError(Object beanInstance, Throwable error) {
        System.err.println("[LIFECYCLE] Error in bean: " +
                (beanInstance != null ? beanInstance.getClass().getSimpleName() : "unknown") +
                " - " + error.getMessage());
    }

    @Override
    public void onContainerInitialized() {
        System.out.println("[LIFECYCLE] Container initialized");
    }

    @Override
    public void onContainerShutdown() {
        System.out.println("[LIFECYCLE] Container shutdown");
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