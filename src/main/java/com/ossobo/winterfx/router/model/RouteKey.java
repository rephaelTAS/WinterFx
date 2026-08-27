package com.ossobo.winterfx.router.model;

import java.util.Objects;

/**
 * Chave única de rota no {@code ApiDispatcher}, composta pelo caminho
 * normalizado (sempre iniciando com "/") e pelo verbo ({@link RouteType}).
 *
 * <p>A introdução da chave composta garante que GET e POST nunca se
 * intercambiarem no despacho: {@code Rotas.post()} jamais atingirá uma
 * rota GET e vice-versa — além de permitir, futuramente, o mesmo caminho
 * registrado em verbos diferentes.</p>
 */
public record RouteKey(String path, RouteType type) {

    public RouteKey {
        Objects.requireNonNull(path, "path não pode ser nulo");
        Objects.requireNonNull(type, "type não pode ser nulo");
    }

    public static RouteKey of(RouteType type, String path) {
        return new RouteKey(path, type);
    }

    @Override
    public String toString() {
        return "[" + type + "] " + path;
    }
}