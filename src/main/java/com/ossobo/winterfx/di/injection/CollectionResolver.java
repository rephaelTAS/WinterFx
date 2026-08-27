package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.di.resolver.DependencyResolver;

import java.lang.reflect.ParameterizedType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolvedor de coleções para injeção de dependências.
 * Compartilhado entre FieldInjector, MethodInjector e DependencyResolver.
 * Versão 2.0 - Java 17+ com Pattern Matching e Switch Expression.
 */
public final class CollectionResolver {

    private final DependencyResolver dependencyResolver;

    public CollectionResolver(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    /**
     * Resolve uma injeção de coleção usando Pattern Matching.
     */
    public Object resolve(java.lang.reflect.Type collectionType, String qualifier) {
        if (collectionType instanceof ParameterizedType pt) {
            var elementType = (Class<?>) pt.getActualTypeArguments()[0];
            var rawType = (Class<?>) pt.getRawType();

            // ✅ USA O QUALIFIER SE FORNECIDO
            List<?> implementations;
            if (qualifier != null && !qualifier.isEmpty()) {
                implementations = List.of(dependencyResolver.getBean(elementType, qualifier));
            } else {
                implementations = dependencyResolver.getAllBeansOfType(elementType);
            }

            return switch (rawType.getSimpleName()) {
                case "List" -> implementations;
                case "Set" -> new HashSet<>(implementations);
                case "Collection" -> implementations;
                default -> throw new IllegalArgumentException(
                        "Tipo de coleção não suportado: " + rawType.getSimpleName() +
                                ". Use List, Set ou Collection.");
            };
        }

        throw new IllegalArgumentException(
                "Coleção deve ser parametrizada: " + collectionType);
    }
}