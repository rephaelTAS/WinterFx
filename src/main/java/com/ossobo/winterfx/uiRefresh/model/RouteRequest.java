package com.ossobo.winterfx.uiRefresh.model;

import java.util.Map;
import java.util.Objects;

/**
 * Envelope imutável que representa a requisição de uma rota interna.
 *
 * <p>Contém a rota destino e o mapa de parâmetros enviados pela camada de visão.
 * É passado para os {@link ParameterResolver} que sabem extrair os valores
 * com base nas anotações {@code @UI}, {@code @Payload} e {@code @RouteVar}.</p>
 */
public final class RouteRequest {

    private final String route;
    private final Map<String, Object> params;
    private final long timestamp;

    public RouteRequest(String route, Map<String, Object> params) {
        this.route    = Objects.requireNonNull(route);
        // Map.copyOf exige um Map não nulo e cria uma cópia imutável
        this.params   = Map.copyOf(params);
        this.timestamp = System.nanoTime();
    }

    public String route()            { return route; }

    public Map<String, Object> params() { return params; }

    public long timestamp()          { return timestamp; }

    public Object get(String key) {
        return params.get(key);
    }

    public boolean has(String key) {
        return params.containsKey(key);
    }
}