package com.ossobo.winterfx.di.exceptions;

import java.util.Set;

/**
 * Lançada quando o resolvedor encontra múltiplas definições de beans
 * para o mesmo tipo, e nenhuma qualificação (@Qualifier) foi fornecida.
 */
public class NoUniqueBeanDefinitionException extends RuntimeException {
    public NoUniqueBeanDefinitionException(String type, Set<String> beanNames) {
        super("Não foi encontrada uma única definição de bean para o tipo '" + type + "'. Candidatos: " + beanNames);
    }
}