/*
 * UserHelpStyle v1.0
 *
 * Constantes visuais unificadas para o módulo de ajuda ao usuário.
 * Cores, estilos e classes CSS compartilhadas entre todos os componentes.
 *
 * Módulo: NexusFX UserHelp
 * v1.0: Versão inicial
 */
package com.ossobo.winterfx.userhelp;

public final class UserHelpStyle {

    private UserHelpStyle() {}

    // ===== CORES =====
    public static final String ERROR_RED = "#e74c3c";
    public static final String ERROR_BG = "#fdf0ef";
    public static final String SUCCESS_GREEN = "#27ae60";
    public static final String SUCCESS_BG = "#eafaf1";
    public static final String WARNING_ORANGE = "#f39c12";
    public static final String WARNING_BG = "#fef9e7";
    public static final String INFO_BLUE = "#3498db";
    public static final String INFO_BG = "#eaf2f8";
    public static final String OVERLAY_BG = "rgba(0, 0, 0, 0.6)";
    public static final String TOAST_BG = "#2c3e50";
    public static final String TOAST_TEXT = "#ffffff";

    // ===== ESTILOS INLINE =====
    public static final String FIELD_ERROR_STYLE =
            "-fx-border-color: " + ERROR_RED + "; " +
                    "-fx-border-width: 2px; " +
                    "-fx-background-color: " + ERROR_BG + ";";

    public static final String FIELD_CLEAR_STYLE =
            "-fx-border-color: transparent; " +
                    "-fx-background-color: transparent;";

    public static final String TOAST_STYLE =
            "-fx-background-color: " + TOAST_BG + "; " +
                    "-fx-text-fill: " + TOAST_TEXT + "; " +
                    "-fx-padding: 12 20; " +
                    "-fx-background-radius: 6; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);";

    public static final String TOOLTIP_ERROR_STYLE =
            "-fx-background-color: " + ERROR_RED + "; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12px;";

    public static final String OVERLAY_STYLE =
            "-fx-background-color: " + OVERLAY_BG + ";";

    public static final String ONBOARDING_BUBBLE_STYLE =
            "-fx-background-color: white; " +
                    "-fx-border-color: " + INFO_BLUE + "; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-padding: 16; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 2, 4);";

    // ===== CLASSES CSS =====
    public static final String CSS_FIELD_ERROR = "field-error";
    public static final String CSS_FIELD_VALID = "field-valid";
    public static final String CSS_TOAST_SUCCESS = "toast-success.fxml";
    public static final String CSS_TOAST_ERROR = "toast-error";
    public static final String CSS_TOAST_INFO = "toast-info";
    public static final String CSS_GUARD_DIALOG = "guard-dialog";
    public static final String CSS_ONBOARDING_OVERLAY = "onboarding-overlay";

    // ===== TEMPOS (ms) =====
    public static final int TOAST_DURATION_SHORT = 3000;   // 3 segundos
    public static final int TOAST_DURATION_DEFAULT = 5000; // 5 segundos
    public static final int TOAST_DURATION_LONG = 8000;    // 8 segundos
    public static final int ANIMATION_DURATION = 300;      // transições
}