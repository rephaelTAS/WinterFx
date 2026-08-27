// Subscription.java v1.0 - Mantido
// Representa uma assinatura ativa no EventBus.
package com.ossobo.winterfx.event;

/**
 * Representa uma assinatura ativa no EventBus.
 * Deve ser descartada quando o controller for destruído para evitar memory leaks.
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * {@code
 * private Subscription subscription;
 *
 * public void init() {
 *     subscription = eventBus.subscribe(MyEvent.class, this::onMyEvent);
 * }
 *
 * public void destroy() {
 *     subscription.dispose();
 * }
 * }
 * </pre>
 */
@FunctionalInterface
public interface Subscription {
    /**
     * Remove a assinatura do EventBus.
     * Deve ser chamado quando o objeto assinante for destruído.
     */
    void dispose();
}