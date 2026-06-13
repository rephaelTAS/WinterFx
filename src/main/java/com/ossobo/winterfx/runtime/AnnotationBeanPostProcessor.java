// Classe AnnotationBeanPostProcessor v1.0 - 2026-06-12
// Aplica proxies WinterFX automaticamente a todos os beans do container.
package com.ossobo.winterfx.runtime;

/**
 * Post-processor que envolve beans com proxies para anotações WinterFX.
 * Registre no container DI para ativação automática.
 */
public class AnnotationBeanPostProcessor implements BeanPostProcessor {

    private final WinterFXProxyFactory proxyFactory;

    public AnnotationBeanPostProcessor(WinterFXProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return proxyFactory.wrap(bean);
    }
}