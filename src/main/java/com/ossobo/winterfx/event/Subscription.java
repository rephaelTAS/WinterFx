package com.ossobo.winterfx.event;

/**
 * Representa uma assinatura ativa no EventBus.
 * Deve ser descartada quando o controller for destruído para evitar memory leaks.
 */
@FunctionalInterface
public interface Subscription {
    void dispose();
}