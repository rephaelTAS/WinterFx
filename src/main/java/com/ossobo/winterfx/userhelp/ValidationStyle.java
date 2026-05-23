/*
 * ValidationStyle v1.0
 *
 * Constantes de estilo para validação visual de campos.
 * Centraliza cores e classes CSS usadas na validação.
 *
 * Módulo: NexusFX Validation
 * v1.0: Versão inicial
 */
package com.ossobo.winterfx.userhelp;

public final class ValidationStyle {

    private ValidationStyle() {} // Classe utilitária

    // Cores
    public static final String ERROR_BORDER_COLOR = "#e74c3c";
    public static final String ERROR_BACKGROUND = "#fdf0ef";
    public static final String SUCCESS_BORDER_COLOR = "#27ae60";

    // Estilos inline (fallback quando CSS externo não cobre)
    public static final String STYLE_ERROR =
            "-fx-border-color: " + ERROR_BORDER_COLOR + "; " +
                    "-fx-border-width: 2px; " +
                    "-fx-background-color: " + ERROR_BACKGROUND + ";";

    public static final String STYLE_CLEAR =
            "-fx-border-color: transparent; " +
                    "-fx-background-color: transparent;";

    // Classe CSS recomendada
    public static final String CSS_CLASS_ERROR = "field-error";
    public static final String CSS_CLASS_VALID = "field-valid";
}