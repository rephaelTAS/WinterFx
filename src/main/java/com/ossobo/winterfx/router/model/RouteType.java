package com.ossobo.winterfx.router.model;

/**
 * Verbo semântico da rota interna, análogo aos métodos HTTP.
 *
 * <p>GET  → leitura, listagens, carregamento de estado.</p>
 * <p>PUT → escrita, ações, mutação de estado.</p>
 */
public enum RouteType {
    GET,
    PUT,
    EXECUTE,
    UI,
    DELETE
}