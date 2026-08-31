package com.ossobo.winterfx.anotations;

import java.lang.annotation.*;

/**
 * Injeta o mapa COMPLETO de parâmetros da requisição como um {@code Params}.
 *
 * <p>Para handlers que aceitam parâmetros dinâmicos (filtros variáveis,
 * pass-through, proxies de rota) — onde declarar cada {@code @RouteVar}
 * seria rígido demais.</p>
 *
 * <pre>{@code
 * @GetMapping("pesquisar")
 * public ResponseData pesquisar(@AllParams Params params) {
 *     var termo   = params.getString("termo");
 *     var pagina  = params.getInt("pagina", 1);      // com default
 *     var filtros = params.getString("filtros");      // opcional: pode ser null
 *     ...
 * }
 * }</pre>
 *
 * <p>Também funciona por CONVENÇÃO: um parâmetro do tipo {@code Params}
 * sem nenhuma outra anotação é resolvido automaticamente como se tivesse
 * {@code @AllParams}.</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AllParams {
}