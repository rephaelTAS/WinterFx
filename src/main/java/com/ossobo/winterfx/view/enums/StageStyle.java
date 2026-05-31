package com.ossobo.winterfx.view.enums;

/**
 * Estilos de janela suportados.
 */
public enum StageStyle {
    DECORATED,
    UNDECORATED,
    TRANSPARENT,
    UNIFIED,
    UTILITY;

    public javafx.stage.StageStyle toJavaFX() {
        return switch (this) {
            case DECORATED   -> javafx.stage.StageStyle.DECORATED;
            case UNDECORATED -> javafx.stage.StageStyle.UNDECORATED;
            case TRANSPARENT -> javafx.stage.StageStyle.TRANSPARENT;
            case UNIFIED     -> javafx.stage.StageStyle.UNIFIED;
            case UTILITY     -> javafx.stage.StageStyle.UTILITY;
        };
    }
}