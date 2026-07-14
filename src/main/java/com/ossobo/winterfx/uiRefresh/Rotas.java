package com.ossobo.winterfx.uiRefresh;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.uiRefresh.model.Params;
import com.ossobo.winterfx.uiRefresh.model.ResponseData;

/**
 * Fachada estática de alto nível para o sistema de rotas internas do WinterFX.
 *
 * <p>Oferece métodos para envio de dados simples, ou execução semântica
 * baseada em anotações (onde a ordem dos parâmetros não importa).</p>
 */
public final class Rotas {

    private Rotas() {}

    /**
     * Executa uma rota casando os parâmetros via anotações (@Payload, @UI, @RouteVar).
     * A ordem dos parâmetros no {@code Params} não precisa bater com a do método.
     */
    public static Object get(String rota, Params params) {
        return WinterApplication.getInstance()
                .getApiDispatcher()
                .dispatch(rota, params.build());
    }

    /**
     * Executa uma rota baseado estritamente na ordem posicional dos argumentos (Legado).
     */
    public static Object get(String rota, Object... argumentos) {
        return WinterApplication.getInstance()
                .getApiDispatcher()
                .dispatch(rota, argumentos);
    }

    /**
     * Busca dados em uma rota GET sem argumentos.
     */
    public static Object get(String rota) {
        return WinterApplication.getInstance()
                .getApiDispatcher()
                .dispatch(rota);
    }

    /**
     * Executa um método pelo nome dentro do controller da rota especificada.
     *
     * @param rota  A rota do controller (ex: "catalogo_form").
     * @param acao O nome do método a ser executado (ex: "clear").
     * @return O que o método retornou, ou ResponseData de erro.
     */
    public static Object executAction(String rota, String acao) {
        return WinterApplication.getInstance()
                .getApiDispatcher()
                .dispatchAction(rota, acao);
    }

    /**
     * Envia dados via POST baseado em ordem posicional (Legado).
     */
    public static Object post(String rota, Object... argumentos) {
        return WinterApplication.getInstance()
                .getApiDispatcher()
                .dispatch(rota, argumentos);
    }

    /**
     * Utilitário para evitar cast manual ao usar o {@code get}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T receiveAs(String rota, Class<T> tipo) {
        Object resultado = get(rota);
        if (resultado == null) return null;
        return (T) resultado;
    }
}