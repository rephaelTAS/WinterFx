package com.ossobo.winterfx.di.annotations.enums;

/**
 * Tipos de comportamento transacional
 */
public enum TransactionalType {
    REQUIRED,    // Usa transação existente ou cria nova
    REQUIRES_NEW, // Sempre cria nova transação
    MANDATORY,   // Exige transação existente
    NEVER        // Nunca executa em transação
}
