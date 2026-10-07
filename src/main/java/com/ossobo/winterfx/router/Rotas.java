package com.ossobo.winterfx.router;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.router.model.Params;
import com.ossobo.winterfx.router.model.ResponseData;
import com.ossobo.winterfx.router.processor.ApiDispatcher;

/**
 * Rotas v1.0 — Fachada estática do roteador interno do WinterFX.
 *
 * <p>Cada método despacha por um CANAL ISOLADO. O lookup é estrito: um verbo
 * jamais enxerga a rota de outro. Handlers registrados com {@code @GetMapping}
 * só respondem a {@link #get}; handlers com {@code @PutMapping} só a
 * {@link #put}; e assim por diante.</p>
 *
 * <h2>Escolha do canal — regra de ouro</h2>
 *
 * <table border="1">
 *   <tr><th>Objetivo</th><th>Canal</th><th>Anotação (handler)</th><th>Fachada</th></tr>
 *   <tr>
 *     <td>Enviar dados para uma view popular (abrir janela já preenchida,
 *         passar filtro, listar, buscar por id)</td>
 *     <td><b>GET</b></td>
 *     <td>{@code @GetMapping}</td>
 *     <td>{@link #get(String, Params)}</td>
 *   </tr>
 *   <tr>
 *     <td>Escrever / atualizar / mutar dados no banco</td>
 *     <td><b>PUT</b></td>
 *     <td>{@code @PutMapping}</td>
 *     <td>{@link #put(String, Params)}</td>
 *   </tr>
 *   <tr>
 *     <td>Remover recurso</td>
 *     <td><b>DELETE</b></td>
 *     <td>{@code @DeleteMapping}</td>
 *     <td>{@link #delete(String, Params)}</td>
 *   </tr>
 *   <tr>
 *     <td>Comando / processo disparado no receptor, sem payload de retorno
 *         relevante (ex.: "refresh", "sincronizar", "exportar")</td>
 *     <td><b>EXEC</b></td>
 *     <td>{@code @ExecMapping}</td>
 *     <td>{@link #exec(String, Params)}</td>
 *   </tr>
 *   <tr>
 *     <td>Transportar componente visual (Node) entre pontos do app — exige
 *         FX Application Thread</td>
 *     <td><b>UI</b></td>
 *     <td>{@code @UiMapping}</td>
 *     <td>{@link #ui(String, Params)}</td>
 *   </tr>
 * </table>
 *
 * <h2>Exemplos</h2>
 *
 * <pre>{@code
 * // GET — enviar dados para outra tela popular (NÃO muta banco)
 * Rotas.get("inventario-detail/carregar/id", Params.with("id", equip.id()));
 * Rotas.get("historico-list/aplicar-prefiltro",
 *           Params.with("sku", "SN-001").and("funcionarioid", "FUNC-042"));
 * Rotas.get("catalogo-produtos/service/por/sku", Params.with("sku", sku));
 *
 * // PUT — mutação (escrita/atualização de estado persistido)
 * Rotas.put("inventario-crud/service/cadastrar", Params.with("equipamento", eq));
 * Rotas.put("catalogo-produtos/service/salvar",  Params.with("produto", p));
 *
 * // DELETE — remoção
 * Rotas.delete("inventario-crud/service/excluir", Params.with("id", id));
 *
 * // EXEC — comando no receptor, sem payload de retorno relevante
 * Rotas.exec("inventario-list/refresh");
 *
 * // UI — mover componente visual (FX Thread)
 * Rotas.ui("tabela-funcionarios/render", Params.with("tabela", tbl));
 * }</pre>
 *
 * <h2>Regra mnemônica</h2>
 * <ul>
 *   <li>Estou <b>escrevendo</b> no banco? → {@code PUT}.</li>
 *   <li>Estou <b>passando dado</b> para outra tela? → {@code GET}.</li>
 *   <li>Estou <b>mandando um comando</b> para outra tela fazer algo? → {@code EXEC}.</li>
 *   <li>Estou <b>movendo um componente</b> visual? → {@code UI}.</li>
 * </ul>
 *
 * <p><b>Anti-padrão:</b> usar {@code PUT} só porque o caminho parece "mudança de
 * tela". Trocar de tela e preencher campos é {@code GET} — não há escrita no
 * banco, e o handler receptor deve declarar {@code @GetMapping}, não
 * {@code @PutMapping}. Canal e anotação sempre casam.</p>
 *
 * @since v1.0
 */
public final class Rotas {

    private Rotas() { }

    // ============================================================
    // GET — leitura / transporte de dado para view popular
    // ============================================================

    /** GET sem parâmetros. */
    public static ResponseData get(String rota) {
        return d().dispatchGet(rota);
    }

    /** GET com parâmetros. */
    public static ResponseData get(String rota, Params params) {
        return d().dispatchGet(rota, params.build());
    }

    // ============================================================
    // PUT — escrita / atualização de estado persistido
    // ============================================================

    /** PUT com parâmetros (o verbo de mutação não tem fachada sem params). */
    public static ResponseData put(String rota, Params params) {
        return d().dispatchPut(rota, params.build());
    }

    // ============================================================
    // DELETE — remoção
    // ============================================================

    /** DELETE sem parâmetros. */
    public static ResponseData delete(String rota) {
        return d().dispatchDelete(rota);
    }

    /** DELETE com parâmetros. */
    public static ResponseData delete(String rota, Params params) {
        return d().dispatchDelete(rota, params.build());
    }

    // ============================================================
    // EXEC — comando / processo
    // ============================================================

    /** EXEC sem parâmetros. */
    public static ResponseData exec(String rota) {
        return d().dispatchExec(rota);
    }

    /** EXEC com parâmetros. */
    public static ResponseData exec(String rota, Params params) {
        return d().dispatchExec(rota, params.build());
    }

    // ============================================================
    // UI — transporte de componente visual (FX Application Thread)
    // ============================================================

    /** UI sem parâmetros. Exige a JavaFX Application Thread. */
    public static ResponseData ui(String rota) {
        return d().dispatchUi(rota);
    }

    /** UI com parâmetros. Exige a JavaFX Application Thread. */
    public static ResponseData ui(String rota, Params params) {
        return d().dispatchUi(rota, params.build());
    }

    // ============================================================
    // ATALHO
    // ============================================================

    /**
     * Conveniência para ler UM dado do envelope de uma rota GET.
     * <pre>{@code
     * List<Livro> livros = Rotas.receiveData("livros/listar", "livros", List.class);
     * }</pre>
     */
    public static <T> T receiveData(String rota, String key, Class<T> type) {
        return get(rota).getData(key, type);
    }

    // ============================================================
    // INTERNO
    // ============================================================

    private static ApiDispatcher d() {
        return WinterApplication.getInstance().getApiDispatcher();
    }
}