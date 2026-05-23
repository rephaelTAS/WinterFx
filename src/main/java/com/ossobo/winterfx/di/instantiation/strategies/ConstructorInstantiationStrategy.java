package com.ossobo.winterfx.di.instantiation.strategies;

import com.ossobo.winterfx.di.annotations.Inject;
import com.ossobo.winterfx.di.exceptions.DependencyResolutionException;
import com.ossobo.winterfx.di.instantiation.InstantiationStrategy;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.di.scanner.models.BeanDefinition;

import java.lang.reflect.Constructor;
import java.util.Arrays;

/**
 * Estratégia de instanciação por construtor.
 * Prioriza @Inject, fallback para construtor padrão.
 */
public final class ConstructorInstantiationStrategy implements InstantiationStrategy {

    private final DependencyResolver dependencyResolver;
    private final ReflectionProcessor reflectionProcessor = new ReflectionProcessor();

    public ConstructorInstantiationStrategy(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    @Override
    public boolean canHandle(BeanDefinition definition) {
        return !definition.isFactoryMethod();
    }

    @Override
    public Object instantiate(BeanDefinition definition) throws Exception {
        Class<?> type = definition.getType();
        Constructor<?> constructor = findConstructor(type);
        Object[] args = resolveArguments(constructor);
        return reflectionProcessor.instantiate(constructor, args);
    }

    private Constructor<?> findConstructor(Class<?> type) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();

        // @Inject
        var annotated = Arrays.stream(constructors)
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .toList();

        if (annotated.size() == 1) return annotated.get(0);
        if (annotated.size() > 1) {
            throw new DependencyResolutionException(
                    "Múltiplos @Inject em: " + type.getName());
        }

        // Construtor padrão
        return Arrays.stream(constructors)
                .filter(c -> c.getParameterCount() == 0)
                .findFirst()
                .orElseGet(() -> {
                    if (constructors.length == 1) return constructors[0];
                    throw new DependencyResolutionException(
                            "Nenhum construtor padrão em: " + type.getName());
                });
    }

    private Object[] resolveArguments(Constructor<?> constructor) {
        return Arrays.stream(constructor.getParameters())
                .map(p -> dependencyResolver.resolve(p.getParameterizedType(), null))
                .toArray();
    }
}
