package com.ossobo.winterfx.di.aop;

import com.ossobo.winterfx.scanner.ReflectionScanner;

import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * ProxyManager v3.2
 *
 * Cria proxies para beans:
 * - Se tiver interface → Proxy JDK (WinterInvocationHandler)
 * - Se NÃO tiver interface → Proxy ByteBuddy (subclasse com target)
 * - Se não puder ser proxyado → retorna o target original
 */
public final class ProxyManager {

    private final ReflectionScanner reflectionScanner;

    public ProxyManager(ReflectionScanner reflectionScanner) {
        this.reflectionScanner = reflectionScanner;
    }

    /**
     * Cria proxy para o bean quando possível.
     * O proxy sempre envolve o target original, preservando dependências.
     */
    public Object createProxyIfNecessary(Object target) {
        if (target == null) return null;

        Class<?> targetClass = target.getClass();

        if (!canProxy(targetClass)) {
            return target;
        }

        List<Class<?>> interfaces = reflectionScanner.getInterfaces(targetClass);

        if (!interfaces.isEmpty()) {
            return Proxy.newProxyInstance(
                    targetClass.getClassLoader(),
                    interfaces.toArray(new Class<?>[0]),
                    new WinterInvocationHandler(target)
            );
        }

        return ByteBuddyProxyFactory.createProxy(target);
    }

    /**
     * Verifica se a classe pode ser proxyada de forma segura.
     */
    public boolean canProxy(Class<?> type) {
        if (type == null) return false;
        if (type.isPrimitive()) return false;
        if (type.isArray()) return false;
        if (type.isEnum()) return false;
        if (type.isAnnotation()) return false;
        if (Modifier.isFinal(type.getModifiers())) return false;
        return true;
    }
}