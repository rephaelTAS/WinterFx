// PipelineInterruptedException.java
package com.ossobo.winterfx.runtime.handler;

/**
 * Exceção lançada para interromper o pipeline de interceptação.
 *
 * <p>Usada por handlers BEFORE quando a operação deve ser cancelada
 * (ex: usuário não confirma ação).</p>
 *
 * <p><b>NÃO</b> aciona handlers de erro como {@code @OnError}.</p>
 *
 * @version 1.0
 */
public class PipelineInterruptedException extends RuntimeException {

    public PipelineInterruptedException(String message) {
        super(message);
    }

    public PipelineInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}