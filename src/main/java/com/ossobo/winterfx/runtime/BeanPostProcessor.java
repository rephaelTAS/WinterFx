// BeanPostProcessor.java v2.0 - 2026-08-22
// Contrato para post-processors no container DI WinterFX
package com.ossobo.winterfx.runtime;

/**
 * Permite modificar beans após instanciação.
 * Inspirado no Spring, mas simplificado e com Java 17+.
 *
 * @version 2.0 (22/08/2026) - Adicionados default methods com documentação
 */
public interface BeanPostProcessor {

    /**
     * Aplica transformação no bean antes da inicialização.
     *
     * @param bean Bean a ser processado
     * @param beanName Nome do bean no container
     * @return Bean processado (pode ser proxy ou wrapper)
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * Aplica transformação no bean após a inicialização.
     *
     * @param bean Bean a ser processado
     * @param beanName Nome do bean no container
     * @return Bean processado (pode ser proxy ou wrapper)
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}