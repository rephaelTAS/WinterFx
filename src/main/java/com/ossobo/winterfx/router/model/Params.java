package com.ossobo.winterfx.router.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Construtor de parâmetros para despacho de rotas — o espelho de entrada do
 * {@link ResponseData}. Transporta dados da VIEW para o CONTROLLER, casando
 * chaves com as anotações {@code @Payload}, {@code @RouteVar} e {@code @UI}.
 *
 * <p><b>API simétrica em 3 níveis</b> (igual ao envelope de retorno):</p>
 *
 * <table>
 *   <tr><th>Operação</th><th>Params (entrada)</th><th>ResponseData (saída)</th></tr>
 *   <tr><td>Escrita tipada</td><td>{@code withInt / withLong / withString...}</td><td>{@code withData}</td></tr>
 *   <tr><td>Leitura simples</td><td>{@code getString / getInt / getLong...}</td><td>{@code getDataString / getDataInt...}</td></tr>
 *   <tr><td>Leitura verificada</td><td>{@code get(key, Tipo.class)}</td><td>{@code getData(key, Tipo.class)}</td></tr>
 *   <tr><td>Leitura obrigatória</td><td>{@code require(key, Tipo.class)}</td><td>{@code requireData(key, Tipo.class)}</td></tr>
 * </table>
 *
 * <p><b>Exemplo completo:</b></p>
 * <pre>{@code
 * // EMISSOR (view) — envio tipado, funciona com var:
 * var params = Params.withString("busca", "machado")
 *                    .withInt("pagina", 1)
 *                    .withLong("categoriaId", 7L)
 *                    .and("livro", livroObjeto);       // objetos via and/with
 * Rotas.get("livros/pesquisar", params);
 *
 * // HANDLER — recebe valores via anotações OU o mapa inteiro:
 * @GetMapping("pesquisar")
 * public ResponseData pesquisar(@RouteVar("busca") String busca,
 *                               @RouteVar("pagina") Integer pagina,
 *                               @AllParams Params todos) {
 *     var categoria = todos.getLong("categoriaId");   // leitura dinâmica
 *     ...
 * }
 * }</pre>
 */
public final class Params {

    private final Map<String, Object> values = new LinkedHashMap<>();

    private Params() {}

    // =====================================================================
    // FÁBRICAS — simples e tipadas
    // =====================================================================

    /** Mapa vazio, pronto para encadear. */
    public static Params empty() {
        return new Params();
    }

    /** Par genérico — aceita qualquer objeto (DTOs, entidades, listas, Nodes). */
    public static Params with(String key, Object value) {
        return new Params().and(key, value);
    }

    /** {@code Params.withString("busca", "machado")} — intenção explícita de String. */
    public static Params withString(String key, String value) {
        return new Params().and(key, value);
    }

    /** {@code Params.withInt("pagina", 1)} — autoboxa para Integer. */
    public static Params withInt(String key, int value) {
        return new Params().and(key, value);
    }

    /** {@code Params.withLong("id", 10L)} — autoboxa para Long. */
    public static Params withLong(String key, long value) {
        return new Params().and(key, value);
    }

    /** {@code Params.withDouble("salario", 3500.0)} — autoboxa para Double. */
    public static Params withDouble(String key, double value) {
        return new Params().and(key, value);
    }

    /** {@code Params.withBool("ativo", true)} — autoboxa para Boolean. */
    public static Params withBool(String key, boolean value) {
        return new Params().and(key, value);
    }

    /**
     * Reconstrói um Params a partir de um mapa (usado pelo Dispatcher para
     * injetar {@code @AllParams}, e útil em testes).
     */
    public static Params from(Map<String, Object> source) {
        var params = new Params();
        if (source != null) {
            params.values.putAll(source);
        }
        return params;
    }

    // =====================================================================
    // ESCRITA FLUENTE
    // =====================================================================

    /**
     * Adiciona um par mantendo o encadeamento.
     *
     * <p><b>Fail-fast:</b> chave {@code null} lança imediatamente —
     * typo de chave nunca mais viaja silenciosamente até o handler.</p>
     *
     * <p>Valores {@code null} são permitidos (significam "campo vazio"),
     * desde que o {@code RouteRequest} use cópia tolerante a null.</p>
     */
    public Params and(String key, Object value) {
        Objects.requireNonNull(key, "chave do parâmetro não pode ser nula");
        values.put(key, value);
        return this;
    }

    /** Atalho tipado: {@code andString("busca", t)} ≡ {@code and("busca", t)}. */
    public Params andString(String key, String value)   { return and(key, value); }
    /** Atalho tipado para int (autoboxa). */
    public Params andInt(String key, int value)         { return and(key, value); }
    /** Atalho tipado para long (autoboxa). */
    public Params andLong(String key, long value)       { return and(key, value); }
    /** Atalho tipado para double (autoboxa). */
    public Params andDouble(String key, double value)   { return and(key, value); }
    /** Atalho tipado para boolean (autoboxa). */
    public Params andBool(String key, boolean value)    { return and(key, value); }

    // =====================================================================
    // LEITURA — 3 níveis (simetria com ResponseData)
    // =====================================================================

    /** Leitura bruta (pode devolver null se a chave não existir). */
    public Object get(String key) {
        return values.get(key);
    }

    /**
     * <b>Leitura verificada.</b> Garante o tipo em runtime:
     * <pre>Param 'pagina': esperado Integer, mas recebido String</pre>
     */
    public <T> T get(String key, Class<T> type) {
        Object v = values.get(key);
        if (v == null) return null;
        if (!type.isInstance(v)) {
            throw new IllegalStateException(String.format(
                    "Param '%s': esperado %s, mas recebido %s",
                    key, type.getSimpleName(), v.getClass().getSimpleName()));
        }
        return type.cast(v);
    }

    /**
     * <b>Leitura obrigatória.</b> Lança se a chave nem existir — para
     * parâmetros sem os quais a rota não faz sentido.
     */
    public <T> T require(String key, Class<T> type) {
        if (!values.containsKey(key)) {
            throw new IllegalStateException(String.format(
                    "Param '%s' ausente — o emissor não enviou este parâmetro "
                            + "(typo no nome?). Parâmetros enviados: %s",
                    key, values.keySet()));
        }
        return get(key, type);
    }

    // ── Wrappers tipados (funcionam com var) ─────────────────────────────

    /** {@code var busca = params.getString("busca");} → String verificado. */
    public String getString(String key) { return get(key, String.class); }

    /** {@code var pagina = params.getInt("pagina");} → Integer verificado. */
    public Integer getInt(String key)   { return get(key, Integer.class); }

    /** {@code var id = params.getLong("id");} → Long verificado. */
    public Long getLong(String key)     { return get(key, Long.class); }

    /** {@code var preco = params.getDouble("preco");} → Double verificado. */
    public Double getDouble(String key) { return get(key, Double.class); }

    /** {@code var ativo = params.getBool("ativo");} → Boolean verificado. */
    public Boolean getBool(String key)  { return get(key, Boolean.class); }

    // ── Com valor padrão (para parâmetros opcionais) ─────────────────────

    /** Devolve o int da chave, ou {@code fallback} se ausente ou null. */
    public int getInt(String key, int fallback) {
        var v = values.get(key);
        return v instanceof Number n ? n.intValue() : fallback;
    }

    /** Devolve o long da chave, ou {@code fallback} se ausente ou null. */
    public long getLong(String key, long fallback) {
        var v = values.get(key);
        return v instanceof Number n ? n.longValue() : fallback;
    }

    /** Devolve a String da chave, ou {@code fallback} se ausente ou null. */
    public String getString(String key, String fallback) {
        var v = values.get(key);
        return v != null ? String.valueOf(v) : fallback;
    }

    // =====================================================================
    // CONSULTA
    // =====================================================================

    /** Indica se a chave foi enviada (mesmo com valor null). */
    public boolean has(String key) { return values.containsKey(key); }

    /** Quantidade de parâmetros. */
    public int size() { return values.size(); }

    /** {@code true} se nenhum parâmetro foi enviado. */
    public boolean isEmpty() { return values.isEmpty(); }

    // =====================================================================
    // SAÍDA
    // =====================================================================

    /**
     * Snapshot imutável do mapa, na ordem de inserção.
     * É uma CÓPIA — mutações posteriores no builder não afetam o mapa
     * já entregue (e vice-versa).
     */
    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /** Alias de {@link #toMap()}, mantido por compatibilidade. */
    public Map<String, Object> build() {
        return toMap();
    }

    @Override
    public String toString() {
        return "Params" + values;
    }
}