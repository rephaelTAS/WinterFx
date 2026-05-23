/*
 * FieldValidator v1.0
 *
 * Validador visual de campos de formulário.
 * Destaca campos inválidos com borda vermelha + tooltip.
 * API fluente: .required().email().custom().validate()
 *
 * Módulo: NexusFX UserHelp
 * v1.0: Versão inicial — extraído do FormValidator original
 */
package com.ossobo.winterfx.userhelp;

import javafx.scene.control.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class FieldValidator {

    private final Map<Control, List<String>> errors = new LinkedHashMap<>();
    private final List<Rule> rules = new ArrayList<>();

    // =========================================================================
    // REGRAS DE VALIDAÇÃO (API FLUENTE)
    // =========================================================================

    /** Campo obrigatório */
    public FieldValidator required(Control field, String fieldName) {
        rules.add(new Rule(field, () -> {
            String value = extractText(field);
            return (value == null || value.trim().isEmpty())
                    ? fieldName + " é obrigatório"
                    : null;
        }));
        return this;
    }

    /** Formato de email */
    public FieldValidator email(Control field) {
        rules.add(new Rule(field, () -> {
            String value = extractText(field);
            if (value == null || value.trim().isEmpty()) return null;
            return Pattern.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", value)
                    ? null : "Email inválido";
        }));
        return this;
    }

    /** Tamanho mínimo */
    public FieldValidator minLength(Control field, int min, String fieldName) {
        rules.add(new Rule(field, () -> {
            String value = extractText(field);
            if (value == null || value.trim().isEmpty()) return null;
            return value.trim().length() < min
                    ? fieldName + " deve ter no mínimo " + min + " caracteres"
                    : null;
        }));
        return this;
    }

    /** Tamanho máximo */
    public FieldValidator maxLength(Control field, int max, String fieldName) {
        rules.add(new Rule(field, () -> {
            String value = extractText(field);
            if (value == null) return null;
            return value.trim().length() > max
                    ? fieldName + " deve ter no máximo " + max + " caracteres"
                    : null;
        }));
        return this;
    }

    /** Validação customizada */
    public FieldValidator custom(Control field, Function<String, Boolean> test, String errorMessage) {
        rules.add(new Rule(field, () -> {
            String value = extractText(field);
            if (value == null || value.trim().isEmpty()) return null;
            return test.apply(value.trim()) ? null : errorMessage;
        }));
        return this;
    }

    /** Duas senhas devem ser iguais */
    public FieldValidator match(Control field, Control confirmField, String fieldName) {
        rules.add(new Rule(confirmField, () -> {
            String v1 = extractText(field);
            String v2 = extractText(confirmField);
            if (v1 == null || v2 == null || v1.isEmpty() || v2.isEmpty()) return null;
            return v1.equals(v2) ? null : fieldName + " não confere";
        }));
        return this;
    }

    // =========================================================================
    // EXECUÇÃO
    // =========================================================================

    /** Executa validação. Retorna true se tudo válido. */
    public boolean validate() {
        clearAllErrors();

        for (Rule rule : rules) {
            String error = rule.supplier.get();
            if (error != null) {
                errors.computeIfAbsent(rule.field, k -> new ArrayList<>()).add(error);
            }
        }

        for (Map.Entry<Control, List<String>> entry : errors.entrySet()) {
            markError(entry.getKey(), String.join("\n", entry.getValue()));
        }

        return errors.isEmpty();
    }

    /** Valida e executa callback se sucesso */
    public void onSubmit(Runnable onSuccess) {
        if (validate()) {
            onSuccess.run();
        }
    }

    // =========================================================================
    // MANIPULAÇÃO VISUAL
    // =========================================================================

    private void markError(Control field, String message) {
        field.setStyle(UserHelpStyle.FIELD_ERROR_STYLE);
        field.getStyleClass().add(UserHelpStyle.CSS_FIELD_ERROR);

        Tooltip tooltip = new Tooltip(message);
        tooltip.setStyle(UserHelpStyle.TOOLTIP_ERROR_STYLE);
        Tooltip.install(field, tooltip);
    }

    private void clearAllErrors() {
        for (Rule rule : rules) {
            Control field = rule.field;
            field.setStyle(UserHelpStyle.FIELD_CLEAR_STYLE);
            field.getStyleClass().remove(UserHelpStyle.CSS_FIELD_ERROR);
            Tooltip.uninstall(field, null);
        }
        errors.clear();
    }

    // =========================================================================
    // INTERNO
    // =========================================================================

    private String extractText(Control field) {
        if (field instanceof TextInputControl textField) return textField.getText();
        if (field instanceof ComboBoxBase<?> combo) {
            Object value = combo.getValue();
            return value != null ? value.toString() : null;
        }
        if (field instanceof Labeled labeled) return labeled.getText();
        return null;
    }

    /** Regra interna: campo + supplier que retorna mensagem de erro ou null */
    private static class Rule {
        final Control field;
        final java.util.function.Supplier<String> supplier;

        Rule(Control field, java.util.function.Supplier<String> supplier) {
            this.field = field;
            this.supplier = supplier;
        }
    }
}