package com.ossobo.winterfx.scanner.models;

import com.ossobo.winterfx.scanner.enums.ScopeType;

import java.lang.reflect.Method;
import java.util.*;
import java.util.Objects;

/**
 * Representa a definição de um Bean no container de injeção de dependências.
 * Transformado em Record para garantir imutabilidade real e eliminação de boilerplate.
 *
 * <p>Armazena todas as informações necessárias para que o {@code DiContainer}
 * possa instanciar, injetar dependências e gerenciar o ciclo de vida do bean.</p>
 *
 * <p>Suporta dois modos de definição:</p>
 * <ul>
 *   <li><b>Component Scanning:</b> beans descobertos via {@code @Component}, {@code @Service}, etc.</li>
 *   <li><b>Factory Method:</b> beans definidos via {@code @Configuration} + {@code @Bean}</li>
 * </ul>
 *
 * @see ScopeType
 * @see InjectionPoint
 */
public record BeanDefinition(
        String name,
        Class<?> type,
        ScopeType scopeType,
        Class<?> factoryClass,
        Method factoryMethod,
        List<InjectionPoint> dependencies,
        Method postConstructMethod,
        Method preDestroyMethod,
        boolean primary,
        String qualifier,
        Map<String, String> values
) {
    /**
     * Construtor compacto para validação centralizada e imutabilidade profunda.
     */
    public BeanDefinition {
        Objects.requireNonNull(name, "name não pode ser nulo");
        Objects.requireNonNull(type, "type não pode ser nulo");
        Objects.requireNonNull(scopeType, "scopeType não pode ser nulo");
        Objects.requireNonNull(dependencies, "dependencies não pode ser nulo");

        // ✅ Torna a lista de dependências profundamente imutável
        dependencies = List.copyOf(dependencies);

        // Garante imutabilidade profunda no mapa de valores
        values = values != null ? Collections.unmodifiableMap(values) : Collections.emptyMap();
    }

    /**
     * Fábrica estática para beans definidos por Component Scanning.
     */
    public static BeanDefinition component(String name, Class<?> type, ScopeType scopeType,
                                           List<InjectionPoint> dependencies,
                                           Method postConstructMethod, Method preDestroyMethod,
                                           boolean primary, String qualifier, Map<String, String> values) {
        return new BeanDefinition(name, type, scopeType, null, null, dependencies,
                postConstructMethod, preDestroyMethod, primary, qualifier, values);
    }

    /**
     * Fábrica estática para beans definidos por Factory Method.
     */
    public static BeanDefinition factory(String name, Class<?> type, ScopeType scopeType,
                                         Class<?> factoryClass, Method factoryMethod,
                                         List<InjectionPoint> dependencies,
                                         Method postConstructMethod, Method preDestroyMethod,
                                         boolean primary, String qualifier, Map<String, String> values) {
        return new BeanDefinition(name, type, scopeType, factoryClass, factoryMethod, dependencies,
                postConstructMethod, preDestroyMethod, primary, qualifier, values);
    }

    /**
     * @return true se o bean usa factory method
     */
    public boolean isFactoryMethod() {
        return factoryMethod != null;
    }

    /**
     * @return true se o bean usa @Scope personalizado
     */
    public boolean hasCustomScope() {
        return scopeType != ScopeType.SINGLETON;
    }

    /**
     * @return lista de nomes de campos com @Value
     */
    public List<String> getValueFieldNames() {
        return List.copyOf(values.keySet());
    }

    /**
     * Retorna a expressão @Value para um campo específico.
     *
     * @param fieldName nome do campo
     * @return expressão @Value, ou null se o campo não tiver @Value
     */
    public String getValueExpression(String fieldName) {
        return values.get(fieldName);
    }

    @Override
    public String toString() {
        String source = isFactoryMethod()
                ? "Factory: " + factoryClass.getSimpleName() + "." + factoryMethod.getName() + "()"
                : "Class: " + type.getName();

        StringBuilder sb = new StringBuilder();
        sb.append("BeanDefinition[name=").append(name)
                .append(", type=").append(type.getSimpleName())
                .append(", scope=").append(scopeType.name())
                .append(", primary=").append(primary);

        if (qualifier != null) {
            sb.append(", qualifier=").append(qualifier);
        }

        if (hasCustomScope()) {
            sb.append(", customScope=true");
        }

        sb.append(", source=").append(source)
                .append(", dependencies=").append(dependencies.size())
                .append(", valueFields=").append(values.size())
                .append("]");

        return sb.toString();
    }
}