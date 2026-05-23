package com.ossobo.winterfx.exceptions;

public class BusinessRuleException extends RuntimeException {
    private final String ruleCode;
    private final String businessContext;

    public BusinessRuleException(String message) {
        super(message);
        this.ruleCode = "UNKNOWN";
        this.businessContext = "GENERAL";
    }

    public BusinessRuleException(String ruleCode, String businessContext, String message) {
        super(String.format("[%s:%s] %s", businessContext, ruleCode, message));
        this.ruleCode = ruleCode;
        this.businessContext = businessContext;
    }

    public String getRuleCode() { return ruleCode; }
    public String getBusinessContext() { return businessContext; }
}
