package com.ossobo.winterfx.anotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação de nível de método utilizada para mapear uma operação de consulta
 * (leitura) no sistema de roteamento interno do WinterFX.
 *
 * <p>Quando aplicada a um método dentro de um controlador anotado com {@code @RequestMapping},
 * o {@link com.ossobo.winterfx.uiRefresh.processor.ApiDispatcher} registará esse método
 * no mapa de rotas do tipo GET. Por convenção do framework, métodos anotados com
 * {@code @GetMapping} devem ser utilizados para buscar dados, listar entidades ou
 * carregar o estado inicial de uma tela, sem causar efeitos colaterais na aplicação.</p>
 *
 * <p>O método anotado <b>deve</b> obrigatoriamente retornar um objeto
 * {@link com.ossobo.winterfx.uiRefresh.model.ResponseData}.</p>
 *
 * @see com.ossobo.winterfx.anotations.RequestMapping
 * @see com.ossobo.winterfx.anotations.PostMapping
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface GetMapping {

    /**
     * O identificador da rota de consulta.
     *
     * <p>Será concatenado com o {@code value()} da anotação {@code @RequestMapping} da classe.</p>
     *
     * @return O valor da rota (ex: "listar", "dados").
     */
    String value() default "";
}