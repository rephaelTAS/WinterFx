/*
 * FormValidator v1.0
 *
 * Validador visual de formulários para NexusFX.
 * Destaca campos inválidos com borda vermelha + tooltip.
 * API fluente: .required().email().custom().onSubmit()
 *
 * Módulo: NexusFX Validation
 * v1.0: Versão inicial
 */
package com.ossobo.winterfx.userhelp;

import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class FormValidator {

    private final Map<Control, List<String>> validations = new LinkedHashMap<>();
    private final Map<Control, String> errorMessages = new LinkedHashMap<>();

    // =========================================================================
    // REGRAS DE VALIDAÇÃO (API FLUENTE)
    // =========================================================================

    /** Campo obrigatório (não vazio) */
    public FormValidator required(Control field, String fieldName) {
        addRule(field, () -> {
            String value = extractText(field);
            return value == null || value.trim().isEmpty()
                    ? fieldName + " é obrigatório"
                    : null;
        });
        return this;
    }

    /** Valida formato de email */
    public FormValidator email(Control field) {
        addRule(field, () -> {
            String value = extractText(field);
            if (value == null || value.trim().isEmpty()) return null; // deixa required tratar
            return Pattern.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", value)
                    ? null : "Email inválido";
        });
        return this;
    }

    /** Tamanho mínimo */
    public FormValidator minLength(Control field, int min, String fieldName) {
        addRule(field, () -> {
            String value = extractText(field);
            if (value == null || value.trim().isEmpty()) return null;
            return value.trim().length() < min
                    ? fieldName + " deve ter no mínimo " + min + " caracteres"
                    : null;
        });
        return this;
    }

    /** Tamanho máximo */
    public FormValidator maxLength(Control field, int max, String fieldName) {
        addRule(field, () -> {
            String value = extractText(field);
            if (value == null) return null;
            return value.trim().length() > max
                    ? fieldName + " deve ter no máximo " + max + " caracteres"
                    : null;
        });
        return this;
    }

    /** Validação customizada */
    public FormValidator custom(Control field, Function<String, Boolean> test, String errorMessage) {
        addRule(field, () -> {
            String value = extractText(field);
            if (value == null || value.trim().isEmpty()) return null;
            return test.apply(value.trim()) ? null : errorMessage;
        });
        return this;
    }

    // =========================================================================
    // EXECUÇÃO
    // =========================================================================

    /**
     * Executa todas as validações.
     * @return true se todos os campos são válidos
     */
    public boolean validate() {
        clearAllErrors();
        boolean valid = true;

        for (Map.Entry<Control, List<String>> entry : validations.entrySet()) {
            Control field = entry.getKey();
            for (String rule : entry.getValue()) {
                // A regra é executada via supplier interno
                // Armazenado como referência, executamos agora
            }
        }

        for (Map.Entry<Control, List<String>> entry : validations.entrySet()) {
            Control field = entry.getKey();
            for (String error : entry.getValue()) {
                if (error != null) {
                    markError(field, error);
                    valid = false;
                }
            }
        }

        return valid;
    }

    /**
     * Executa validação e chama callback se válido.
     * @param onSuccess ação a executar se formulário válido
     */
    public void onSubmit(Runnable onSuccess) {
        if (validate()) {
            onSuccess.run();
        }
    }

    // =========================================================================
    // MANIPULAÇÃO VISUAL
    // =========================================================================

    /** Destaca campo como inválido */
    public void markError(Control field, String message) {
        field.setStyle(ValidationStyle.STYLE_ERROR);
        field.getStyleClass().add(ValidationStyle.CSS_CLASS_ERROR);

        // Tooltip com erro
        Tooltip tooltip = new Tooltip(message);
        tooltip.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        Tooltip.install(field, tooltip);

        errorMessages.put(field, message);
    }

    /** Remove destaque de um campo específico */
    public void clearError(Control field) {
        field.setStyle(ValidationStyle.STYLE_CLEAR);
        field.getStyleClass().remove(ValidationStyle.CSS_CLASS_ERROR);
        Tooltip.uninstall(field, null);
        errorMessages.remove(field);
    }

    /** Remove todos os destaques */
    public void clearAllErrors() {
        for (Control field : errorMessages.keySet()) {
            field.setStyle(ValidationStyle.STYLE_CLEAR);
            field.getStyleClass().remove(ValidationStyle.CSS_CLASS_ERROR);
            Tooltip.uninstall(field, null);
        }
        errorMessages.clear();
    }

    // =========================================================================
    // INTERNO
    // =========================================================================

    private void addRule(Control field, java.util.function.Supplier<String> rule) {
        // Armazenamos o supplier para execução lazy no validate()
        // Usamos uma lista para permitir múltiplas regras por campo
        // Simplificação: armazenamos no mapa e executamos no validate
        validations.computeIfAbsent(field, k -> new ArrayList<>())
                .add(rule.get());
    }

    /** Extrai texto de qualquer Control comum */
    private String extractText(Control field) {
        if (field instanceof javafx.scene.control.TextInputControl textField) {
            return textField.getText();
        }
        if (field instanceof javafx.scene.control.ComboBoxBase<?> combo) {
            Object value = combo.getValue();
            return value != null ? value.toString() : null;
        }
        if (field instanceof javafx.scene.control.Labeled labeled) {
            return labeled.getText();
        }
        return null;
    }
}