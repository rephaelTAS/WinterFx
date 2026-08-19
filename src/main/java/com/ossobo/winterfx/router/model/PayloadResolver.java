package com.ossobo.winterfx.router.model;

import com.ossobo.winterfx.anotations.Payload;
import java.lang.reflect.Parameter;

public final class PayloadResolver implements ParameterResolver {

    public static final String DEFAULT_KEY = "payload";

    @Override
    public boolean supports(Parameter p) {
        return p.isAnnotationPresent(Payload.class);
    }

    @Override
    public Object resolve(Parameter p, RouteRequest request) {
        Payload ann = p.getAnnotation(Payload.class);
        String key  = ann.value().isBlank() ? DEFAULT_KEY : ann.value();
        Object value = request.get(key);

        if (value == null) {
            return null;
        }
        if (!p.getType().isAssignableFrom(value.getClass())) {
            throw new RouteBindingException(String.format(
                    "Payload incompatível: esperado %s, recebido %s",
                    p.getType().getSimpleName(), value.getClass().getSimpleName()));
        }
        return value;
    }
}