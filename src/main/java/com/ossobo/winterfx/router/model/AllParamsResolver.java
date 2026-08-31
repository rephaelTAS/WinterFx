package com.ossobo.winterfx.router.model;

import com.ossobo.winterfx.anotations.AllParams;

import java.lang.reflect.Parameter;
import java.util.Arrays;

/**
 * Injeta o mapa completo de parâmetros como {@code Params}.
 *
 * <p>Ativação: anotação {@code @AllParams} explícita, OU parâmetro do tipo
 * {@code Params} sem nenhuma outra anotação de binding (convenção).</p>
 *
 * <p><b>Registrar por ÚLTIMO</b> na lista de resolvers do Dispatcher —
 * os resolvers específicos têm prioridade.</p>
 */
public final class AllParamsResolver implements ParameterResolver {

    @Override
    public boolean supports(Parameter p) {
        if (p.isAnnotationPresent(AllParams.class)) {
            return true;
        }
        // Convenção: Params sem nenhuma anotação de binding → recebe tudo
        return Params.class.isAssignableFrom(p.getType())
                && Arrays.stream(p.getAnnotations()).noneMatch(a ->
                a instanceof com.ossobo.winterfx.anotations.Payload
                        || a instanceof com.ossobo.winterfx.anotations.UI
                        || a instanceof com.ossobo.winterfx.anotations.RouteVar);
    }

    @Override
    public Object resolve(Parameter p, RouteRequest request) {
        return Params.from(request.params());
    }
}