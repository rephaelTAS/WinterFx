package com.ossobo.winterfx.router;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.router.model.Params;
import com.ossobo.winterfx.router.model.ResponseData;
import com.ossobo.winterfx.router.processor.ApiDispatcher;

/**
 * Fachada estática do roteador WinterFX. Todo despacho devolve o envelope
 * {@link ResponseData} — não há cast do lado do chamador, nunca.
 */
public final class Rotas {

    private Rotas() {}

    // ---------- GET: leitura ----------
    public static ResponseData get(String rota)                   { return d().dispatchGet(rota); }
    public static ResponseData get(String rota, Params params)    { return d().dispatchGet(rota, params.build()); }

    // ---------- PUT: escrita/atualização ----------
    public static ResponseData put(String rota, Params params)    { return d().dispatchPut(rota, params.build()); }

    // ---------- DELETE: remoção ----------
    public static ResponseData delete(String rota)                { return d().dispatchDelete(rota); }
    public static ResponseData delete(String rota, Params params) { return d().dispatchDelete(rota, params.build()); }

    // ---------- EXEC: comandos/processos ----------
    public static ResponseData exec(String rota)                  { return d().dispatchExec(rota); }
    public static ResponseData exec(String rota, Params params)   { return d().dispatchExec(rota, params.build()); }

    // ---------- UI: transporte de componentes visuais (FX Thread!) ----------
    public static ResponseData ui(String rota)                    { return d().dispatchUi(rota); }
    public static ResponseData ui(String rota, Params params)     { return d().dispatchUi(rota, params.build()); }

    /**
     * One-liner para quando só precisa de UM valor do envelope.
     * {@code List<Livro> livros = Rotas.receiveData("livros/listar", "livros", List.class);}
     */
    public static <T> T receiveData(String rota, String key, Class<T> type) {
        return get(rota).getData(key, type);
    }

    private static ApiDispatcher d() {
        return WinterApplication.getInstance().getApiDispatcher();
    }
}