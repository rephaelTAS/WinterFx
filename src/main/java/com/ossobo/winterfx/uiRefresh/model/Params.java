package com.ossobo.winterfx.uiRefresh.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Construtor fluido (Builder) para montar o mapa de parâmetros enviado às rotas.
 *
 * <p>Elimina a necessidade de usar {@code new HashMap<>()} e garante que o desenvolvedor
 * defina chaves textuais explicitamente, que serão casadas com as anotações
 * {@code @RouteVar}, {@code @UI} e {@code @Payload} no Controller.</p>
 *
 * <p><b>Exemplo de uso:</b></p>
 * <pre>
 * Params.with("id", 10L)
 *       .and("usuario", meuUsuario)
 *       .and("painelForm", meuPainel)
 *       .build();
 * </pre>
 */
public final class Params {

    private final Map<String, Object> parametros = new HashMap<>();

    private Params() {}

    /**
     * Ponto de entrada estático para iniciar a construção dos parâmetros.
     *
     * @param key   A chave que será casada com a anotação no Controller (ex: "id").
     * @param value O objeto sendo enviado (ex: 10L, um Pane, uma Lista).
     * @return A instância de {@code Params} para encadeamento.
     */
    public static Params with(String key, Object value) {
        return new Params().and(key, value);
    }

    /**
     * Adiciona um novo par de chave-valor ao mapa de parâmetros.
     *
     * @param key   A chave de identificação.
     * @param value O objeto sendo enviado.
     * @return A própria instância de {@code Params}.
     */
    public Params and(String key, Object value) {
        parametros.put(key, value);
        return this;
    }

    /**
     * Finaliza a construção e retorna o mapa imutável que será processado pelo Dispatcher.
     *
     * @return O mapa contendo todos os parâmetros.
     */
    public Map<String, Object> build() {
        return parametros;
    }
}