// AnnotationHandler.java v3.0 - 2026-08-22
// Interface com pipeline condicional - removido método supports() redundante
package com.ossobo.winterfx.runtime.handler;

import java.lang.annotation.Annotation;

/**
 * Interface para handlers de anotações runtime com pipeline condicional.
 *
 * <p>Cada anotação ({@code @OnSuccess}, {@code @NewScene}, etc.)
 * tem seu próprio handler que implementa esta interface.</p>
 *
 * <p><b>Pipeline de Interceptação:</b></p>
 * <ol>
 *   <li><b>FASE BEFORE:</b> executa handlers com {@link #isBeforePhase()} = true</li>
 *   <li><b>EXECUÇÃO:</b> método executa e captura exceção (se houver)</li>
 *   <li><b>FASE AFTER:</b>
 *     <ul>
 *       <li>Se erro: executa handlers com {@link #isErrorOnly()} = true</li>
 *       <li>Se sucesso: executa handlers com {@link #isSuccessOnly()} = true</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * @param <A> Tipo da anotação que este handler processa
 *
 * @version 3.0 (22/08/2026) - Removido método supports() redundante
 */
public interface AnnotationHandler<A extends Annotation> {

    boolean supports(Annotation annotation);

    /**
     * @return A classe da anotação que este handler processa
     */
    Class<A> getAnnotationType();

    /**
     * Processa a anotação encontrada no método.
     *
     * @param context Contexto de anotação com método, alvo, argumentos e resultado/exceção
     * @param annotation Anotação a processar
     */
    void handle(AnnotationContext context, A annotation);

    // ========== Faseamento ==========

    /**
     * Verifica se este handler executa na fase BEFORE.
     */
    default boolean isBeforePhase() {
        return false;
    }

    /**
     * Verifica se este handler executa na fase AFTER.
     */
    default boolean isAfterPhase() {
        return true;
    }

    // ========== Filtros de Resultado ==========

    /**
     * Verifica se este handler executa SÓ se método sucesso.
     */
    default boolean isSuccessOnly() {
        return false;
    }

    /**
     * Verifica se este handler executa SÓ se método erro.
     */
    default boolean isErrorOnly() {
        return false;
    }
}