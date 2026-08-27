package com.ossobo.winterfx.router;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.router.model.Params;
import com.ossobo.winterfx.router.model.RouteBindingException;
import com.ossobo.winterfx.router.processor.ApiDispatcher;

public final class Rotas {

    private Rotas() {}

    // ---------- GET: leitura ----------
    public static Object get(String rota)                { return d().dispatchGet(rota); }
    public static Object get(String rota, Params params) { return d().dispatchGet(rota, params.build()); }

    // ---------- PUT: escrita/atualização ----------
    public static Object put(String rota, Params params) { return d().dispatchPut(rota, params.build()); }
    public static Object put(String rota, Object... args){ return d().dispatchPut(rota, args); }

    // ---------- DELETE: remoção ----------
    public static Object delete(String rota, Params params) { return d().dispatchDelete(rota, params.build()); }

    // ---------- EXEC: comandos/processos ----------
    public static Object exec(String rota)               { return d().dispatchExec(rota); }
    public static Object exec(String rota, Params params){ return d().dispatchExec(rota, params.build()); }

    // ---------- UI: TRANSPORTE de componentes visuais ----------
    public static Object ui(String rota, Params params)  { return d().dispatchUi(rota, params.build()); }
    public static Object ui(String rota)                 { return d().dispatchUi(rota); }

    // ---------- auxiliares ----------
    public static Object executeAction(String rota, String action) { return d().dispatchAction(rota, action); }

    @SuppressWarnings("unchecked")
    public static <T> T receiveAs(String rota, Class<T> tipo) {
        Object result = get(rota);
        if (result == null) return null;
        if (!tipo.isInstance(result)) {
            throw new RouteBindingException(
                    "Rota '" + rota + "' retornou " + result.getClass().getSimpleName()
                            + ", esperava " + tipo.getSimpleName());
        }
        return (T) result;
    }

    private static ApiDispatcher d() {
        return WinterApplication.getInstance().getApiDispatcher();
    }
}