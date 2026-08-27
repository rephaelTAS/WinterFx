package com.ossobo.winterfx.scanner.models;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

/**
 * Representa um ponto de injeção de dependência descoberto pelo scanner.
 * Transformado em Record para garantir imutabilidade.
 *
 * <p>Suporta quatro tipos de injeção:</p>
 * <ul>
 *   <li>{@link InjectionType#FIELD} — injeção direta no campo</li>
 *   <li>{@link InjectionType#METHOD} — injeção via método (setter)</li>
 *   <li>{@link InjectionType#CONSTRUCTOR} — injeção via construtor inteiro</li>
 *   <li>{@link InjectionType#CONSTRUCTOR_PARAMETER} — injeção em parâmetro específico de construtor</li>
 *   <li>{@link InjectionType#METHOD_PARAMETER} — injeção em parâmetro específico de método</li>
 * </ul>
 *
 * @see InjectionType
 */
public record InjectionPoint(
        InjectionType type,
        Field field,
        Method method,
        Constructor<?> constructor,
        int parameterIndex,
        Parameter parameter
) {
    /**
     * Construtor compacto para validação centralizada.
     */
    public InjectionPoint {
        Objects.requireNonNull(type, "type não pode ser nulo");

        if (type == InjectionType.CONSTRUCTOR_PARAMETER || type == InjectionType.METHOD_PARAMETER) {
            Objects.requireNonNull(parameter, "parameter não pode ser nulo");
            if (parameterIndex < 0) {
                throw new IllegalArgumentException("parameterIndex deve ser >= 0");
            }
        }
    }

    /**
     * @return true se é injeção em campo
     */
    public boolean isField() {
        return type == InjectionType.FIELD;
    }

    /**
     * @return true se é injeção em método
     */
    public boolean isMethod() {
        return type == InjectionType.METHOD;
    }

    /**
     * @return true se é injeção em construtor
     */
    public boolean isConstructor() {
        return type == InjectionType.CONSTRUCTOR;
    }

    /**
     * @return true se é injeção em parâmetro de construtor
     */
    public boolean isConstructorParameter() {
        return type == InjectionType.CONSTRUCTOR_PARAMETER;
    }

    /**
     * @return true se é injeção em parâmetro de método
     */
    public boolean isMethodParameter() {
        return type == InjectionType.METHOD_PARAMETER;
    }

    /**
     * Cria InjectionPoint para injeção em campo.
     */
    public static InjectionPoint forField(Field field) {
        Objects.requireNonNull(field, "field não pode ser nulo");
        return new InjectionPoint(InjectionType.FIELD, field, null, null, -1, null);
    }

    /**
     * Cria InjectionPoint para injeção em método (setter).
     */
    public static InjectionPoint forMethod(Method method) {
        Objects.requireNonNull(method, "method não pode ser nulo");
        return new InjectionPoint(InjectionType.METHOD, null, method, null, -1, null);
    }

    /**
     * Cria InjectionPoint para injeção em construtor inteiro.
     */
    public static InjectionPoint forConstructor(Constructor<?> constructor) {
        Objects.requireNonNull(constructor, "constructor não pode ser nulo");
        return new InjectionPoint(InjectionType.CONSTRUCTOR, null, null, constructor, -1, null);
    }

    /**
     * Cria InjectionPoint para injeção em parâmetro específico de construtor.
     */
    public static InjectionPoint forConstructorParameter(
            Constructor<?> constructor,
            int parameterIndex,
            Parameter parameter
    ) {
        Objects.requireNonNull(constructor, "constructor não pode ser nulo");
        Objects.requireNonNull(parameter, "parameter não pode ser nulo");

        if (parameterIndex < 0) {
            throw new IllegalArgumentException("parameterIndex deve ser >= 0");
        }

        return new InjectionPoint(InjectionType.CONSTRUCTOR_PARAMETER, null, null, constructor, parameterIndex, parameter);
    }

    /**
     * Cria InjectionPoint para injeção em parâmetro específico de método.
     */
    public static InjectionPoint forMethodParameter(
            Method method,
            int parameterIndex,
            Parameter parameter
    ) {
        Objects.requireNonNull(method, "method não pode ser nulo");
        Objects.requireNonNull(parameter, "parameter não pode ser nulo");

        if (parameterIndex < 0) {
            throw new IllegalArgumentException("parameterIndex deve ser >= 0");
        }

        return new InjectionPoint(InjectionType.METHOD_PARAMETER, null, method, null, parameterIndex, parameter);
    }

    @Override
    public String toString() {
        return switch (type) {
            case FIELD -> "InjectionPoint[FIELD, field=" + field.getName() + "]";
            case METHOD -> "InjectionPoint[METHOD, method=" + method.getName() + "]";
            case CONSTRUCTOR -> "InjectionPoint[CONSTRUCTOR, constructor=" + constructor + "]";
            case CONSTRUCTOR_PARAMETER -> "InjectionPoint[CONSTRUCTOR_PARAMETER, paramIndex=" + parameterIndex
                    + ", param=" + parameter.getName() + "]";
            case METHOD_PARAMETER -> "InjectionPoint[METHOD_PARAMETER, method=" + method.getName()
                    + ", paramIndex=" + parameterIndex + ", param=" + parameter.getName() + "]";
        };
    }
}