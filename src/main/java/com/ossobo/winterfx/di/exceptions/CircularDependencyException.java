package com.ossobo.winterfx.di.exceptions;

/**
 * Lançada quando o resolvedor detecta um ciclo infinito de dependências.
 */
public class CircularDependencyException extends RuntimeException {
    public CircularDependencyException(String dependencyCycle) {
        super("Dependência Circular Detectada: " + dependencyCycle);
    }
}