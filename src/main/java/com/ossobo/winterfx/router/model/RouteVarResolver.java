package com.ossobo.winterfx.router.model;

import com.ossobo.winterfx.anotations.RouteVar;
import java.lang.reflect.Parameter;

public final class RouteVarResolver implements ParameterResolver {

    @Override
    public boolean supports(Parameter p) {
        return p.isAnnotationPresent(RouteVar.class);
    }

    @Override
    public Object resolve(Parameter p, RouteRequest request) {
        RouteVar ann = p.getAnnotation(RouteVar.class);
        String key   = ann.value();
        Object value = request.get(key);

        if (value == null) {
            if (p.getType().isPrimitive()) {
                throw new RouteBindingException(
                        "Parâmetro @" + key + " ausente para tipo primitivo " + p.getType());
            }
            return null;
        }

        Class<?> expected = boxed(p.getType());
        if (!expected.isInstance(value)) {
            throw new RouteBindingException(String.format(
                    "Tipo incompatível em @RouteVar(\"%s\"): esperado %s, recebido %s",
                    key, expected.getSimpleName(), value.getClass().getSimpleName()));
        }
        return value;
    }

    private static Class<?> boxed(Class<?> t) {
        if (t == int.class)     return Integer.class;
        if (t == long.class)    return Long.class;
        if (t == boolean.class) return Boolean.class;
        if (t == double.class)  return Double.class;
        if (t == float.class)   return Float.class;
        if (t == short.class)   return Short.class;
        if (t == byte.class)    return Byte.class;
        if (t == char.class)    return Character.class;
        return t;
    }
}