package com.ossobo.winterfx.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para especificar o arquivo de propriedades a ser carregado.
 *
 * <p>Exemplo de uso:</p>
 * <pre>
 * &#64;Configuration
 * &#64;PropertySource("application.properties")
 * public class AppConfig {
 *     // ...
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PropertySource {
    /**
     * Caminho do arquivo de propriedades no classpath.
     */
    String value();

    /**
     * Se true, falha se o arquivo não for encontrado.
     * Se false, apenas loga um warning.
     */
    boolean failIfNotFound() default true;
}