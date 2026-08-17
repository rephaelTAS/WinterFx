package com.ossobo.winterfx.uiRefresh.model;

import java.lang.reflect.Parameter;

public interface ParameterResolver {

    boolean supports(Parameter parameter);

    // Não precisa de 'throws', pois RuntimeException é não-checada,
    // mas se quiser ser super didético pode deixar:
    // Object resolve(Parameter parameter, RouteRequest request) throws RouteBindingException;
    Object resolve(Parameter parameter, RouteRequest request);
}