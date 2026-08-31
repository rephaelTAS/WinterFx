package com.ossobo.winterfx.router.model;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.*;

/**
 * Envelope padrão para o transporte de dados no sistema de roteamento interno do WinterFX.
 *
 * <p>Análogo ao {@code ResponseEntity} do Spring MVC, adaptado para JavaFX. Todo handler
 * retorna uma instância desta classe, que carrega quatro espaços independentes:</p>
 *
 * <table>
 *   <tr><th>Espaço</th><th>Escrita</th><th>Leitura simples</th><th>Leitura verificada</th></tr>
 *   <tr><td>dados (primitivos, objetos, listas)</td><td>{@code withData}</td><td>{@code getData(key)}</td><td>{@code getData(key, Tipo.class)}</td></tr>
 *   <tr><td>componentes visuais (Node)</td><td>{@code withUi}</td><td>{@code getUi(key)}</td><td>{@code getUi(key, Tipo.class)}</td></tr>
 *   <tr><td>mensagem global</td><td>{@code withMessage}</td><td>{@code getMessage()}</td><td>—</td></tr>
 *   <tr><td>erros por campo</td><td>{@code withError}</td><td>{@code getFirstError()} / {@code getError(campo)}</td><td>—</td></tr>
 * </table>
 *
 * <p><b>Regra de segurança:</b> coleções entregues via {@code withData} são copiadas
 * defensivamente e seladas como imutáveis — o receptor NUNCA consegue mutar o estado
 * interno do controller que enviou. Componentes visuais ({@code withUi}) não são copiados:
 * Node não tem cópia e o transporte visual é justamente a entrega da mesma instância.</p>
 *
 * <p><b>Exemplo completo:</b></p>
 * <pre>{@code
 * // CONTROLLER (quem responde):
 * return ResponseData.success()
 *         .withMessage("Livro salvo com sucesso!")
 *         .withData("id", salvo.getId())
 *         .withData("livros", service.findAll())
 *         .withUi("badge", new Label(total + " itens"));
 *
 * // VIEW (quem consome):
 * var resp = Rotas.put("livros/salvar", Params.with("payload", livro));
 * if (resp.isSuccess()) {
 *     Long id = resp.getData("id", Long.class);
 *     List<Livro> livros = resp.getDataList("livros");
 *     Label badge = resp.getUi("badge", Label.class);
 * } else {
 *     new Alert(Alert.AlertType.ERROR, resp.getFirstError()).show();
 * }
 * }</pre>
 */
public final class ResponseData {

    private boolean success = true;
    private String message;
    private final Map<String, Object> data   = new LinkedHashMap<>();
    private final Map<String, Node>   ui     = new LinkedHashMap<>();
    private final Map<String, String> errors = new LinkedHashMap<>();

    // =====================================================================
    // FÁBRICAS
    // =====================================================================

    /** Resposta de sucesso ({@code success = true}). */
    public static ResponseData success() {
        return new ResponseData();
    }

    /**
     * Resposta de erro rápida ({@code success = false}) com mensagem global.
     * Erros por campo podem ser adicionados em seguida com {@link #withError}.
     */
    public static ResponseData error(String message) {
        var response = new ResponseData();
        response.success = false;
        response.message = message;
        return response;
    }

    // =====================================================================
    // ESCRITA
    // =====================================================================

    /**
     * Adiciona um dado ao envelope. Aceita primitivos (autoboxados), objetos,
     * listas e mapas.
     *
     * <p><b>Cópia defensiva automática:</b> se o valor for uma coleção
     * ({@code List}, {@code Set}, {@code Map}, etc.), ela é copiada e selada
     * como imutável. O receptor recebe um snapshot — mutações dele não afetam
     * o remetente (e vice-versa). Objetos comuns e Nodes passam como referência.</p>
     *
     * @param key   A chave identificadora do dado (ex: "livros", "total").
     * @param value O valor a transportar (pode ser {@code null}).
     * @return A própria instância, para encadeamento.
     */
    public ResponseData withData(String key, Object value) {
        data.put(key, defensiveCopy(value));
        return this;
    }

    /**
     * Entrega um componente visual JavaFX no canal UI do envelope.
     *
     * <p>O bound {@code extends Node} garante verificação em COMPILE-TIME:
     * {@code withUi("x", "texto")} não compila.</p>
     *
     * <p><b>Não copia:</b> o transporte visual entrega a instância real do Node
     * (é esse o objetivo do canal — mover componentes entre pontos do app).</p>
     *
     * @param key  A chave identificadora do componente (ex: "badge", "tabela").
     * @param node O componente JavaFX.
     * @return A própria instância, para encadeamento.
     */
    public ResponseData withUi(String key, Node node) {
        ui.put(key, node);
        return this;
    }

    /** Define a mensagem global da resposta (sucesso ou erro). */
    public ResponseData withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Adiciona um erro de validação de negócio a um campo específico.
     * Ex: {@code withError("titulo", "Campo obrigatório")}.
     */
    public ResponseData withError(String field, String message) {
        this.errors.put(field, message);
        return this;
    }

    // =====================================================================
    // LEITURA DE DADOS — 3 NÍVEIS
    // =====================================================================

    /**
     * <b>Nível 1 — simples (unchecked).</b> O tipo é inferido da variável de
     * atribuição; NÃO há verificação em runtime. Uso: {@code List<Livro> l = resp.getData("livros");}
     *
     * <p>Não funciona com {@code var} (T infere como Object) — para var, use
     * os wrappers {@code getDataString}, {@code getDataLong}, etc.</p>
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) data.get(key);
    }

    /**
     * <b>Nível 2 — verificada.</b> Garante em runtime que o valor é do tipo
     * esperado. Em caso de divergência, lança {@link IllegalStateException}
     * NA LINHA DA LEITURA, com chave e tipos na mensagem:
     * <pre>Dado 'livro': esperado Livro, mas a rota entregou String</pre>
     */
    public <T> T getData(String key, Class<T> type) {
        Object v = data.get(key);
        if (v == null) return null;
        if (!type.isInstance(v)) {
            throw new IllegalStateException(String.format(
                    "Dado '%s': esperado %s, mas a rota entregou %s",
                    key, type.getSimpleName(), v.getClass().getSimpleName()));
        }
        return type.cast(v);
    }

    /**
     * <b>Nível 3 — obrigatória.</b> Igual a {@link #getData(String, Class)},
     * mas lança também se a chave nem existir no envelope (typo do nome,
     * controller esqueceu de enviar) — fecha o buraco do null silencioso.
     */
    public <T> T requireData(String key, Class<T> type) {
        requirePresence(key);
        return getData(key, type);
    }

    /**
     * Recupera uma lista tipada. Verifica que é uma List (não o tipo do
     * elemento — limitação do type erasure). Com a cópia defensiva do
     * {@code withData}, a lista devolvida é sempre imutável; para popular
     * uma TableView, converta: {@code FXCollections.observableArrayList(resp.getDataList("livros"))}
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getDataList(String key) {
        return (List<T>) getData(key, List.class);
    }

    /** Versão obrigatória de {@link #getDataList(String)} — lança se a chave não existir. */
    @SuppressWarnings("unchecked")
    public <T> List<T> requireDataList(String key) {
        requirePresence(key);
        return (List<T>) getData(key, List.class);
    }

    // ── Wrappers para uso com var ────────────────────────────────────────

    /** {@code var nome = resp.getDataString("nome");} → String verificado. */
    public String  getDataString(String key) { return getData(key, String.class); }

    /** {@code var total = resp.getDataInt("total");} → Integer verificado. */
    public Integer getDataInt(String key)    { return getData(key, Integer.class); }

    /** {@code var id = resp.getDataLong("id");} → Long verificado. */
    public Long    getDataLong(String key)   { return getData(key, Long.class); }

    /** {@code var ativo = resp.getDataBool("ativo");} → Boolean verificado. */
    public Boolean getDataBool(String key)   { return getData(key, Boolean.class); }

    /** {@code var preco = resp.getDataDouble("preco");} → Double verificado. */
    public Double  getDataDouble(String key) { return getData(key, Double.class); }

    // =====================================================================
    // LEITURA DE UI — 3 NÍVEIS
    // =====================================================================

    /**
     * <b>Nível 1 — simples (unchecked).</b>
     *
     * <p><b>Atenção:</b> no canal UI este nível é traiçoeiro — como todo
     * componente é Node, um Label entregue onde se esperava ProgressBar
     * passa sem NENHUMA exceção e entra silenciosamente no layout errado.
     * Prefira {@link #getUi(String, Class)}.</p>
     *
     * <p>Não funciona com {@code var} (T infere como Node).</p>
     */
    @SuppressWarnings("unchecked")
    public <T extends Node> T getUi(String key) {
        return (T) ui.get(key);
    }

    /**
     * <b>Nível 2 — verificada (PADRÃO do canal UI).</b> Garante em runtime que
     * o componente é do tipo esperado:
     * <pre>UI 'barra': esperado ProgressBar, mas a rota entregou Label</pre>
     *
     * <p>Combina com var: {@code var barra = resp.getUi("barra", ProgressBar.class);}</p>
     */
    public <T extends Node> T getUi(String key, Class<T> type) {
        Node v = ui.get(key);
        if (v == null) return null;
        if (!type.isInstance(v)) {
            throw new IllegalStateException(String.format(
                    "UI '%s': esperado %s, mas a rota entregou %s",
                    key, type.getSimpleName(), v.getClass().getSimpleName()));
        }
        return type.cast(v);
    }

    /** <b>Nível 3 — obrigatória.</b> Lança se a chave não existir no canal UI. */
    public <T extends Node> T requireUi(String key, Class<T> type) {
        if (!ui.containsKey(key)) {
            throw new IllegalStateException(String.format(
                    "UI '%s' não existe na resposta — o controller não entregou este componente "
                            + "(typo no nome? faltou withUi?). Componentes enviados: %s",
                    key, ui.keySet()));
        }
        return getUi(key, type);
    }

    // ── Wrappers para uso com var ────────────────────────────────────────

    /** {@code var badge = resp.getUiLabel("badge");} → Label verificado. */
    public Label getUiLabel(String key) { return getUi(key, Label.class); }

    /** {@code var btn = resp.getUiButton("btn-ok");} → Button verificado. */
    public Button getUiButton(String key) { return getUi(key, Button.class); }

    // (adicione outros controles no mesmo padrão se o projeto exigir:
    //  TextField, TableView, ComboBox...)

    // =====================================================================
    // MENSAGENS E ESTADO
    // =====================================================================

    /** {@code true} se a operação teve sucesso. Guarda de todo o consumo. */
    public boolean isSuccess() { return success; }

    /** Mensagem global da resposta (sucesso ou erro), ou {@code null}. */
    public String getMessage() { return message; }

    /** Primeiro erro de validação — ideal para Alert rápido. */
    public String getFirstError() {
        return errors.values().stream().findFirst().orElse(null);
    }

    /** Erro de validação de um campo específico (ex: "titulo", "isbn"). */
    public String getError(String field) {
        return errors.get(field);
    }

    /** Indica se há erros de validação por campo. */
    public boolean hasErrors() { return !errors.isEmpty(); }

    // =====================================================================
    // ACESSORES DE MAPA (compatibilidade)
    // =====================================================================

    /**
     * Mapa de dados completo, imutável. Mantido para compatibilidade —
     * prefira os métodos tipados ({@code getData(key, Tipo.class)}, {@code getDataList}).
     */
    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    /** Mapa de erros por campo, imutável. */
    public Map<String, String> getErrors() {
        return Collections.unmodifiableMap(errors);
    }

    @Override
    public String toString() {
        return "ResponseData{success=" + success
                + (message != null ? ", message='" + message + "'" : "")
                + ", data=" + data.keySet()
                + ", ui=" + ui.keySet()
                + ", errors=" + errors
                + '}';
    }

    // =====================================================================
    // HELPERS PRIVADOS
    // =====================================================================

    private void requirePresence(String key) {
        if (!data.containsKey(key)) {
            throw new IllegalStateException(String.format(
                    "Chave '%s' não existe na resposta — o controller não enviou este dado "
                            + "(typo no nome? faltou withData?). Dados enviados: %s",
                    key, data.keySet()));
        }
    }

    /**
     * Cópia defensiva: coleções viram snapshots imutáveis; todo o resto
     * passa como referência. {@code List.copyOf} tem a vantagem de reaproveitar
     * a instância quando a fonte já é imutável — mas rejeita nulls internos,
     * por isso usamos a cópia tolerante elemento a elemento.
     */
    private static Object defensiveCopy(Object value) {
        if (value instanceof List<?> list)      return Collections.unmodifiableList(new ArrayList<>(list));
        if (value instanceof Set<?> set)        return Collections.unmodifiableSet(new HashSet<>(set));
        if (value instanceof Map<?, ?> map)     return Collections.unmodifiableMap(new LinkedHashMap<>(map));
        if (value instanceof Collection<?> col) return Collections.unmodifiableList(new ArrayList<>(col));
        return value;
    }
}