package com.ossobo.winterfx.anotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação de nível de classe utilizada para definir o mapeamento base (prefixo de rota)
 * para um controlador no sistema de roteamento interno do WinterFX.
 *
 * <p>No WinterFX, o conceito de "rotas" não está vinculado a protocolos HTTP, mas sim
 * a um mecanismo interno de despacho (Dispatcher) baseado em reflexão. Quando um controlador
 * é anotado com {@code @RequestMapping}, o prefixo definido é concatenado com as rotas
 * individuais dos métodos anotados com {@code @GetMapping} ou {@code @PostMapping}.</p>
 *
 * <p><b>Exemplo de concatenação:</b></p>
 * <pre>
 * &#64;RequestMapping("livros")
 * public class LivroController {
 *
 *     &#64;GetMapping("listar")
 *     public ResponseData listar() { ... }
 * }
 * </pre>
 * <p>No exemplo acima, a rota final registrada no despachante interno será
 * <strong>"livros/listar"</strong>.</p>
 *
 * @see com.ossobo.winterfx.anotations.GetMapping
 * @see com.ossobo.winterfx.anotations.PostMapping
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface RequestMapping {

    /**
     * Define o prefixo de rota para todos os métodos deste controlador.
     *
     * <p>Se leftado em branco (""), os métodos usarão apenas o valor definido
     * nas suas respectivas anotações de método.</p>
     *
     * @return O prefixo da rota.
     */
    String value() default "";

    /**
     * Nome amigável ou descritivo do controlador.
     *
     * <p>Reservado para uso futuro em ferramentas de depuração, geração automática
     * de documentação de API interna, logs estruturados ou painéis de diagnóstico.</p>
     *
     * @return O nome descritivo do controlador.
     */
    String name() default "";
}