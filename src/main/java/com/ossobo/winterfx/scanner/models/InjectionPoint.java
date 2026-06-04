package com.ossobo.winterfx.scanner.models;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

import static java.lang.reflect.AccessFlag.Location.METHOD_PARAMETER;

/**
 * Representa um ponto de injeção de dependência descoberto pelo scanner.
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
 * <p>Para injeção em parâmetros, use os métodos fábricas:</p>
 * <ul>
 *   <li>{@link #forConstructorParameter(Constructor, int, Parameter)}</li>
 *   <li>{@link #forMethodParameter(Method, int, Parameter)}</li>
 * </ul>
 *
 * @see InjectionType
 */
public class InjectionPoint {

    private final InjectionType type;
    private final Field field;
    private final Method method;
    private final Constructor<?> constructor;
    private final int parameterIndex;
    private final Parameter parameter;

    /**
     * Construtor para campos, métodos e construtores completos.
     *
     * @param type tipo de injeção
     * @param field campo de injeção (para FIELD)
     * @param method método de injeção (para METHOD)
     * @param constructor construtor de injeção (para CONSTRUCTOR)
     */
    public InjectionPoint(InjectionType type, Field field, Method method, Constructor<?> constructor) {
        this.type = Objects.requireNonNull(type, "type não pode ser nulo");
        this.field = field;
        this.method = method;
        this.constructor = constructor;
        this.parameterIndex = -1;
        this.parameter = null;
    }

    /**
     * Construtor para parâmetros de construtor/método.
     *
     * @param type tipo de injeção (CONSTRUCTOR_PARAMETER ou METHOD_PARAMETER)
     * @param field campo (null para parâmetros)
     * @param method método (para METHOD_PARAMETER)
     * @param constructor construtor (para CONSTRUCTOR_PARAMETER)
     * @param parameterIndex índice do parâmetro
     * @param parameter objeto Parameter
     */
    private InjectionPoint(InjectionType type, Field field, Method method, Constructor<?> constructor,
                           int parameterIndex, Parameter parameter) {
        this.type = Objects.requireNonNull(type, "type não pode ser nulo");
        this.field = field;
        this.method = method;
        this.constructor = constructor;
        this.parameterIndex = parameterIndex;
        this.parameter = Objects.requireNonNull(parameter, "parameter não pode ser nulo");
    }

    /**
     * @return tipo de injeção
     */
    public InjectionType getType() {
        return type;
    }

    /**
     * @return campo de injeção (para FIELD), ou null
     */
    public Field getField() {
        return field;
    }

    /**
     * @return método de injeção (para METHOD e METHOD_PARAMETER), ou null
     */
    public Method getMethod() {
        return method;
    }

    /**
     * @return construtor de injeção (para CONSTRUCTOR e CONSTRUCTOR_PARAMETER), ou null
     */
    public Constructor<?> getConstructor() {
        return constructor;
    }

    /**
     * @return índice do parâmetro (para CONSTRUCTOR_PARAMETER e METHOD_PARAMETER), ou -1
     */
    public int getParameterIndex() {
        return parameterIndex;
    }

    /**
     * @return objeto Parameter (para CONSTRUCTOR_PARAMETER e METHOD_PARAMETER), ou null
     */
    public Parameter getParameter() {
        return parameter;
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
     *
     * @param field campo de injeção
     * @return novo InjectionPoint
     */
    public static InjectionPoint forField(Field field) {
        Objects.requireNonNull(field, "field não pode ser nulo");
        return new InjectionPoint(InjectionType.FIELD, field, null, null);
    }

    /**
     * Cria InjectionPoint para injeção em método (setter).
     *
     * @param method método de injeção
     * @return novo InjectionPoint
     */
    public static InjectionPoint forMethod(Method method) {
        Objects.requireNonNull(method, "method não pode ser nulo");
        return new InjectionPoint(InjectionType.METHOD, null, method, null);
    }

    /**
     * Cria InjectionPoint para injeção em construtor inteiro.
     *
     * @param constructor construtor de injeção
     * @return novo InjectionPoint
     */
    public static InjectionPoint forConstructor(Constructor<?> constructor) {
        Objects.requireNonNull(constructor, "constructor não pode ser nulo");
        return new InjectionPoint(InjectionType.CONSTRUCTOR, null, null, constructor);
    }

    /**
     * Cria InjectionPoint para injeção em parâmetro específico de construtor.
     *
     * @param constructor construtor contendo o parâmetro
     * @param parameterIndex índice do parâmetro no construtor
     * @param parameter objeto Parameter
     * @return novo InjectionPoint
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
     *
     * @param method método contendo o parâmetro
     * @param parameterIndex índice do parâmetro no método
     * @param parameter objeto Parameter
     * @return novo InjectionPoint
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
        switch (type) {
            case FIELD:
                return "InjectionPoint[FIELD, field=" + field.getName() + "]";
            case METHOD:
                return "InjectionPoint[METHOD, method=" + method.getName() + "]";
            case CONSTRUCTOR:
                return "InjectionPoint[CONSTRUCTOR, constructor=" + constructor + "]";
            case CONSTRUCTOR_PARAMETER:
                return "InjectionPoint[CONSTRUCTOR_PARAMETER, paramIndex=" + parameterIndex
                        + ", param=" + parameter.getName() + "]";
            case METHOD_PARAMETER:
                return "InjectionPoint[METHOD_PARAMETER, method=" + method.getName()
                        + ", paramIndex=" + parameterIndex + ", param=" + parameter.getName() + "]";
            default:
                return "InjectionPoint[type=" + type + "]";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InjectionPoint that = (InjectionPoint) o;
        return type == that.type
                && parameterIndex == that.parameterIndex
                && Objects.equals(field, that.field)
                && Objects.equals(method, that.method)
                && Objects.equals(constructor, that.constructor)
                && Objects.equals(parameter, that.parameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, field, method, constructor, parameterIndex, parameter);
    }
}