package com.ossobo.winterfx.di.annotations.enums;

/**
 * Níveis de isolamento para transações
 */
public enum IsolationLevel {
    DEFAULT, READ_UNCOMMITTED, READ_COMMITTED,
    REPEATABLE_READ, SERIALIZABLE
}
