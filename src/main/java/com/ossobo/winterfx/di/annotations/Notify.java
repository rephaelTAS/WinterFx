package com.ossobo.winterfx.di.annotations;

import com.ossobo.winterfx.di.annotations.enums.Modalidade;
import com.ossobo.winterfx.di.annotations.enums.NotificationType;

import java.lang.annotation.*;

/**
 * 🔔 Dispara uma notificação automaticamente após a execução do método.
 *
 * <p>Comportamento:
 * <ul>
 *   <li><b>onException=false</b> (padrão): Dispara ao final do método com sucesso</li>
 *   <li><b>onException=true</b>: Dispara apenas se o método lançar exceção</li>
 *   <li><b>modalidade=MODAL</b>: Diálogo de CONFIRMAÇÃO (Sim/Não) - o método só executa se confirmado!</li>
 *   <li><b>modalidade=NAO_MODAL</b>: Notificação flutuante que some sozinha</li>
 * </ul>
 *
 * <pre>
 * {@code
 * // ✅ SUCESSO: notificação some em 3 segundos
 * @Notify(titulo="Sucesso!", descricao="Salvo!", tipo=SUCCESS)
 * public void salvar() { ... }
 *
 * // ❌ ERRO: só dispara se der exceção (MODAL)
 * @Notify(titulo="Erro!", descricao="Falha!", tipo=ERROR, onException=true)
 * public void excluir() { ... }
 *
 * // ⚠️ CONFIRMAÇÃO: pergunta antes de executar (MODAL)!
 * @Notify(titulo="Confirmar?", descricao="Tem certeza?", tipo=WARNING, modalidade=MODAL)
 * public void excluir(Long id) { ... }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Notify {

    /** Título da notificação */
    String titulo() default "Sucesso";

    /** Descrição/mensagem principal */
    String descricao();

    /** Detalhes técnicos (opcional - para erros) */
    String detalhes() default "";

    /** Tipo da notificação */
    NotificationType tipo() default NotificationType.INFO;

    /**
     * Modalidade da notificação.
     * MODAL = Diálogo de confirmação (Sim/Não), bloqueia tudo
     * NAO_MODAL = Notificação flutuante, some sozinha
     */
    Modalidade modalidade() default Modalidade.NAO_MODAL;

    /** Duração em ms (0 = até fechar manualmente) */
    long duracao() default 3000;

    /** Se true, dispara apenas quando ocorre exceção */
    boolean onException() default false;
}