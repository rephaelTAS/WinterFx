package com.ossobo.winterfx.di.instantiation.strategies;

import com.ossobo.winterfx.di.instantiation.InstantiationStrategy;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.scanner.models.BeanDefinition;


import java.lang.reflect.Method;

/**
 * Estratégia de instanciação por factory method (@Bean).
 */
public final class FactoryMethodStrategy implements InstantiationStrategy {

    private final DependencyResolver dependencyResolver;

    public FactoryMethodStrategy(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    @Override
    public boolean canHandle(BeanDefinition definition) {
        return definition.isFactoryMethod();
    }

    @Override
    public Object instantiate(BeanDefinition definition) throws Exception {
        Class<?> factoryClass = definition.getFactoryClass();
        Method factoryMethod = definition.getFactoryMethod();

        Object factoryInstance = dependencyResolver.getBean(factoryClass);
        Object[] args = resolveArguments(factoryMethod);
        factoryMethod.setAccessible(true);

        return factoryMethod.invoke(factoryInstance, args);
    }

    private Object[] resolveArguments(Method method) {
        return java.util.stream.IntStream.range(0, method.getParameterCount())
                .mapToObj(i -> dependencyResolver.resolve(
                        method.getParameters()[i].getParameterizedType(), null))
                .toArray();
    }
}