// Intercepted.java v1.0 - 2026-08-22
// Anotação marcadora para métodos interceptados
package com.ossobo.winterfx.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca métodos que devem ser interceptados pelo pipeline WinterFX.
 *
 * <p>Métodos com esta anotação são envolvidos por proxy e passam
 * pelo pipeline de interceptação (BEFORE → EXECUÇÃO → AFTER).</p>
 *
 * @version 1.0 (22/08/2026)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Intercepted {
}