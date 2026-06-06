package com.ossobo.winterfx.di.aop;

import com.ossobo.winterfx.runtime.AnnotationRuntime;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ByteBuddyProxyFactory {

    private static final ConcurrentMap<Class<?>, Class<?>> PROXY_CACHE = new ConcurrentHashMap<>();

    private ByteBuddyProxyFactory() {}

    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target) {
        if (target == null) return null;

        Class<?> targetClass = target.getClass();

        if (Modifier.isFinal(targetClass.getModifiers())) {
            return target;
        }

        try {
            Class<?> proxyClass = PROXY_CACHE.computeIfAbsent(targetClass, clazz ->
                    new ByteBuddy()
                            .subclass(clazz)
                            .defineField("proxiedInstance", clazz, Visibility.PRIVATE)
                            .defineMethod("getProxiedInstance", clazz, Visibility.PUBLIC)
                            .intercept(FieldAccessor.ofField("proxiedInstance"))
                            .defineMethod("setProxiedInstance", void.class, Visibility.PUBLIC)
                            .withParameters(clazz)
                            .intercept(FieldAccessor.ofField("proxiedInstance"))
                            .method(
                                    ElementMatchers.isPublic()
                                            .and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class)))
                                            .and(ElementMatchers.not(ElementMatchers.named("getProxiedInstance")))
                                            .and(ElementMatchers.not(ElementMatchers.named("setProxiedInstance")))
                            )
                            .intercept(MethodDelegation.to(Interceptor.class))
                            .make()
                            .load(clazz.getClassLoader())
                            .getLoaded()
            );

            Object proxy = proxyClass.getDeclaredConstructor().newInstance();
            Method setter = proxyClass.getMethod("setProxiedInstance", targetClass);
            setter.invoke(proxy, target);
            return (T) proxy;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar proxy para " + targetClass.getName(), e);
        }
    }

    public static final class Interceptor {

        @RuntimeType
        public static Object intercept(
                @Origin Method method,
                @AllArguments Object[] args,
                @FieldValue("proxiedInstance") Object target) {

            if (target == null) {
                throw new IllegalStateException(
                        "Proxy ByteBuddy ainda não inicializado para o método " + method.getName()
                );
            }

            return AnnotationRuntime.dispatch(target, method.getName(), args);
        }
    }
}