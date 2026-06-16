// AnnotationHandler.java v2.0 - 2026-06-14
// Interface para handlers de anotações runtime com pipeline condicional.
//
// PIPELINE CONDICIONAL v2.0:
//   - isBeforePhase(): handler executa na fase BEFORE
//   - isAfterPhase(): handler executa na fase AFTER
//   - isSuccessOnly(): handler executa SÓ se método sucesso (@OnSuccess, @NewScene, @SwapFxml)
//   - isErrorOnly(): handler executa SÓ se método erro (@OnError, @OnException)
//
// Vantagens v2.0:
//   - ✅ @OnError e @OnSuccess MUTUAMENTE EXCLUSIVOS
//   - ✅Handlers BEFORE/AFTER claros
//   - ✅Handlers SUCCESS_ONLY/ERROR_ONLY filtrados
//   - ✅Compatível com AnnotationContext com erro/resultado
//
// @version 2.0 - Pipeline condicional com faseamento e filtros de resultado
package com.ossobo.winterfx.runtime.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

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
 * <p><b>Exemplo de implementação:</b></p>
 * <pre>
 * {@code
 * public class OnSuccessHandler implements AnnotationHandler<OnSuccess> {
 *     @Override
 *     public void handle(AnnotationContext ctx, OnSuccess annotation) {
 *         notificationManager.showSuccess(annotation.titulo(), annotation.descricao());
 *     }
 *
 *     @Override public boolean isAfterPhase() { return true; }
 *     @Override public boolean isSuccessOnly() { return true; }  // SÓ sucesso
 * }
 * }
 * </pre>
 *
 * @param <A> Tipo da anotação que este handler processa
 *
 * @version 2.0 - Pipeline condicional com faseamento e filtros de resultado
 */
public interface AnnotationHandler<A extends Annotation> {

    /**
     * Verifica se este handler processa a anotação.
     *
     * @param annotation Anotação a verificar
     * @return true se processa, false se não processa
     */
    boolean supports(Annotation annotation);

    /**
     * @return A classe da anotação que este handler processa
     */
    Class<A> getAnnotationType();

    /**
     * Processa a anotação encontrada no método.
     *
     * <p><b>Contexto:</b></p>
     * <ul>
     *   <li>FASE BEFORE: {@link AnnotationContext#getResult()} = null, {@link AnnotationContext#getError()} = null</li>
     *   <li>FASE AFTER (sucesso): {@link AnnotationContext#getResult()} = resultado do método, {@link AnnotationContext#getError()} = null</li>
     *   <li>FASE AFTER (erro): {@link AnnotationContext#getResult()} = null, {@link AnnotationContext#getError()} = exceção</li>
     * </ul>
     *
     * @param context Contexto de anotação com método, alvo, argumentos e resultado/exceção
     * @param annotation Anotação a processar
     */
    void handle(AnnotationContext context, A annotation);

    // ========== NOVO: Faseamento ==========

    /**
     * Verifica se este handler executa na fase BEFORE.
     *
     * <p><b>FASE BEFORE:</b> executa antes do método, sem resultado/exceção.</p>
     *
     * <p><b>Handlers BEFORE:</b></p>
     * <ul>
     *   <li>{@code @OnConfirmation} - validação preliminary</li>
     * </ul>
     *
     * @return true se executa na fase BEFORE, false se não
     */
    default boolean isBeforePhase() {
        return false;
    }

    /**
     * Verifica se este handler executa na fase AFTER.
     *
     * <p><b>FASE AFTER:</b> executa após o método, com resultado ou exceção.</p>
     *
     * <p><b>Handlers AFTER:</b></p>
     * <ul>
     *   <li>{@code @OnSuccess} - notificação de sucesso</li>
     *   <li>{@code @OnError} - notificação de erro</li>
     *   <li>{@code @NewScene} - navegação</li>
     *   <li>{@code @SwapFxml} - troca FXML</li>
     * </ul>
     *
     * @return true se executa na fase AFTER, false se não
     */
    default boolean isAfterPhase() {
        return true;
    }

    // ========== NOVO: Filtros de Resultado ==========

    /**
     * Verifica se este handler executa SÓ se método sucesso.
     *
     * <p>Handler executa apenas quando:</p>
     * <ul>
     *   <li>{@link #isAfterPhase()} = true</li>
     *   <li>Método executou sem exceção</li>
     *   <li>{@link AnnotationContext#getResult()} != null</li>
     * </ul>
     *
     * <p><b>Handlers SUCCESS_ONLY:</b></p>
     * <ul>
     *   <li>{@code @OnSuccess} - mostra notificação de sucesso</li>
     *   <li>{@code @NewScene} - navega para nova view</li>
     *   <li>{@code @SwapFxml} - troca FXML</li>
     * </ul>
     *
     * @return true se executa SÓ se sucesso, false se não
     */
    default boolean isSuccessOnly() {
        return false;
    }

    /**
     * Verifica se este handler executa SÓ se método erro.
     *
     * <p>Handler executa apenas quando:</p>
     * <ul>
     *   <li>{@link #isAfterPhase()} = true</li>
     *   <li>Método lançou exceção</li>
     *   <li>{@link AnnotationContext#getError()} != null</li>
     * </ul>
     *
     * <p><b>Handlers ERROR_ONLY:</b></p>
     * <ul>
     *   <li>{@code @OnError} - mostra notificação de erro</li>
     *   <li>{@code @OnException} - processa exceção</li>
     * </ul>
     *
     * @return true se executa SÓ se erro, false se não
     */
    default boolean isErrorOnly() {
        return false;
    }
}