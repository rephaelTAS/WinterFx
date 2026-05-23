package com.ossobo.winterfx.di.aop;


import com.ossobo.winterfx.di.scanner.ReflectionScanner;

import java.lang.reflect.Proxy;
import java.util.List;

/**
 * ProxyManager v2.0
 *
 * Responsabilidade única: criar proxies AOP para beans.
 *
 * Usa JDK Dynamic Proxy para beans que implementam interfaces.
 * Para classes sem interface, retorna o target sem proxy (por enquanto).
 *
 * @since 2.0
 */
public final class ProxyManager {

    private final ReflectionScanner reflectionScanner;

    public ProxyManager(ReflectionScanner reflectionScanner) {
        this.reflectionScanner = reflectionScanner;
    }

    /**
     * Cria um proxy AOP para o bean, se ele implementar interfaces.
     * Caso contrário, retorna o target original.
     *
     * @param target instância do bean
     * @return proxy ou target original
     */
    public Object createProxyIfNecessary(Object target) {
        Class<?> targetClass = target.getClass();

        List<Class<?>> interfaces = reflectionScanner.getInterfaces(targetClass);

        if (interfaces.isEmpty()) {
            // Sem interfaces → sem proxy JDK
            return target;
        }

        return Proxy.newProxyInstance(
                targetClass.getClassLoader(),
                interfaces.toArray(new Class[0]),
                new DIInvocationHandler(target)
        );
    }

    /**
     * Verifica se um tipo pode receber proxy.
     */
    public boolean canProxy(Class<?> type) {
        return !reflectionScanner.getInterfaces(type).isEmpty();
    }
}