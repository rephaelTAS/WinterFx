package com.ossobo.winterfx.router.model;

import com.ossobo.winterfx.anotations.RouteVar;

import java.lang.reflect.Parameter;

/**
 * RouteVarResolver v3.0.
 * Java 17+ com Switch Expression e Pattern Matching.
 */
public final class RouteVarResolver implements ParameterResolver {

    @Override
    public boolean supports(Parameter p) {
        return p.isAnnotationPresent(RouteVar.class);
    }

    @Override
    public Object resolve(Parameter p, RouteRequest request) {
        var ann = p.getAnnotation(RouteVar.class);
        var key = ann.value();
        var value = request.get(key);

        if (value == null) {
            if (p.getType().isPrimitive()) {
                throw new RouteBindingException(
                        "Parâmetro @" + key + " ausente para tipo primitivo " + p.getType());
            }
            return null;
        }

        var expected = boxed(p.getType());
        if (!expected.isInstance(value)) {
            throw new RouteBindingException(String.format(
                    "Tipo incompatível em @RouteVar(\"%s\"): esperado %s, recebido %s",
                    key, expected.getSimpleName(), value.getClass().getSimpleName()));
        }
        return value;
    }

    /**
     * ✅ Switch Expression com Pattern Matching para boxing.
     */
    private static Class<?> boxed(Class<?> type) {
        return switch (type.getName()) {
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "boolean" -> Boolean.class;
            case "double" -> Double.class;
            case "float" -> Float.class;
            case "short" -> Short.class;
            case "byte" -> Byte.class;
            case "char" -> Character.class;
            default -> type;
        };
    }
}