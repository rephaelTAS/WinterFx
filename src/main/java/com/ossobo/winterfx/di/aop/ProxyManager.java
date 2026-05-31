package com.ossobo.winterfx.di.aop;

import com.ossobo.winterfx.scanner.ReflectionScanner;
import com.ossobo.winterfx.notifications.NotificationInterceptor;

import java.lang.reflect.Proxy;
import java.util.List;

public class ProxyManager {

    private final ReflectionScanner reflectionScanner;
    private final NotificationInterceptor notificationInterceptor;  // 🆕

    public ProxyManager(ReflectionScanner reflectionScanner,
                        NotificationInterceptor notificationInterceptor) {
        this.reflectionScanner = reflectionScanner;
        this.notificationInterceptor = notificationInterceptor;
    }

    public Object createProxyIfNecessary(Object target) {
        Class<?> targetClass = target.getClass();
        List<Class<?>> interfaces = reflectionScanner.getInterfaces(targetClass);

        if (interfaces.isEmpty()) {
            return target;
        }

        return Proxy.newProxyInstance(
                targetClass.getClassLoader(),
                interfaces.toArray(new Class[0]),
                new DIInvocationHandler(target, notificationInterceptor)  // 🆕
        );
    }
}