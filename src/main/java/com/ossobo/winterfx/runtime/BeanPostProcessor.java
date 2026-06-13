// Interface BeanPostProcessor v1.0 - 2026-06-12
// Contrato para post-processors no container DI WinterFX.
package com.ossobo.winterfx.runtime;

/**
 * Permite modificar beans após instanciação.
 * Inspirado no Spring, mas simplificado.
 */
public interface BeanPostProcessor {

    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}