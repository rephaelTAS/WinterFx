// AnnotationBeanPostProcessor.java v2.0 - 2026-08-22
// Post-processor que envolve beans com proxies para anotações WinterFX
package com.ossobo.winterfx.runtime;

/**
 * Post-processor que envolve beans com proxies para anotações WinterFX.
 * Registre no container DI para ativação automática.
 *
 * <p>Implementa Fail-Fast: exceções na criação de proxy são propagadas.</p>
 *
 * @version 2.0 (22/08/2026) - Fail-Fast e validação de null
 */
public class AnnotationBeanPostProcessor implements BeanPostProcessor {

    private final WinterFXProxyFactory proxyFactory;

    public AnnotationBeanPostProcessor(WinterFXProxyFactory proxyFactory) {
        if (proxyFactory == null) {
            throw new IllegalArgumentException("proxyFactory não pode ser null");
        }
        this.proxyFactory = proxyFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean == null) {
            return null;
        }

        // FAIL-FAST: exceções são propagadas (não engolidas)
        return proxyFactory.wrap(bean);
    }
}