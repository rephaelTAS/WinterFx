package com.ossobo.winterfx.core.componnet;


import com.ossobo.winterfx.AlertSystem.model.TipoAlerta;

/**
 * Mapeamento direto para os níveis do NexusFX
 */
public enum AlertLevel {
    CRITICAL(TipoAlerta.CRITICAL),
    ERROR(TipoAlerta.ERRO),
    WARN(TipoAlerta.WARN),
    INFO(TipoAlerta.INFO);

    private final TipoAlerta nexusType;

    AlertLevel(TipoAlerta nexusType) {
        this.nexusType = nexusType;
    }

    public TipoAlerta getNexusType() {
        return nexusType;
    }
}
