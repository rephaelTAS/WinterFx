package com.ossobo.winterfx.di.annotations.enums;

/**
 * Estratégias de inicialização thread-safe
 */
public enum InitializationStrategy {
    EAGER,              // Criado durante inicialização do container
    DOUBLE_CHECKED_LOCKING, // Padrão thread-safe
    ENUM,               // Padrão Joshua Bloch (para singletons estáticos)
    HOLDER              // Padrão Bill Pugh (Lazy Initialization Holder)
}
