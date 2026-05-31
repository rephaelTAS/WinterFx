package com.ossobo.winterfx.di.enums;

/**
 * Modos de destruição para beans em escopo
 */
public enum DestructionMode {
    DEFAULT,            // Destruição normal
    IMMEDIATE,          // Destruir imediatamente
    PHASED,             // Destruição em fases
    MANUAL              // Requer destruição explícita
}
