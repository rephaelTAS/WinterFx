package com.ossobo.winterfx.exceptions;

public class ValidationException extends RuntimeException {
    private final String field;
    private final String validationRule;

    public ValidationException(String message) {
        super(message);
        this.field = null;
        this.validationRule = null;
    }

    public ValidationException(String field, String validationRule, String message) {
        super(String.format("Campo '%s': %s (%s)", field, message, validationRule));
        this.field = field;
        this.validationRule = validationRule;
    }

    public String getField() { return field; }
    public String getValidationRule() { return validationRule; }
}
