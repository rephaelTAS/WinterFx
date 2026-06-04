package com.ossobo.winterfx.scanner.models;

/**
 * Tipos de injeção suportados pelo WinterFX.
 *
 * <p>Define os diferentes pontos de injeção que o container de DI pode resolver:</p>
 * <ul>
 *   <li>{@link #FIELD} — injeção direta no campo (@Inject em campo)</li>
 *   <li>{@link #METHOD} — injeção via método (@Inject em setter)</li>
 *   <li>{@link #CONSTRUCTOR} — injeção via construtor inteiro (@Inject em construtor)</li>
 *   <li>{@link #CONSTRUCTOR_PARAMETER} — injeção em parâmetro específico de construtor</li>
 *   <li>{@link #METHOD_PARAMETER} — injeção em parâmetro específico de método</li>
 * </ul>
 *
 * @see InjectionPoint
 */
public enum InjectionType {
    /**
     * Injeção direta em campo.
     *
     * <p>Exemplo:</p>
     * <pre>{@code
     * @Inject
     * private UsuariosService usuariosService;
     * }</pre>
     */
    FIELD,

    /**
     * Injeção via método (geralmente setter).
     *
     * <p>Exemplo:</p>
     * <pre>{@code
     * @Inject
     * public void setUsuariosService(UsuariosService service) {
     *     this.usuariosService = service;
     * }
     * }</pre>
     */
    METHOD,

    /**
     * Injeção via construtor inteiro (todos os parâmetros).
     *
     * <p>Exemplo:</p>
     * <pre>{@code
     * @Inject
     * public LoginController(UsuariosService usuariosService) {
     *     this.usuariosService = usuariosService;
     * }
     * }</pre>
     */
    CONSTRUCTOR,

    /**
     * Injeção em parâmetro específico de construtor.
     *
     * <p>Usado quando apenas alguns parâmetros do construtor têm @Inject.</p>
     *
     * <p>Exemplo:</p>
     * <pre>{@code
     * public LoginController(@Inject UsuariosService usuariosService, String config) {
     *     this.usuariosService = usuariosService;
     *     this.config = config;
     * }
     * }</pre>
     */
    CONSTRUCTOR_PARAMETER,

    /**
     * Injeção em parâmetro específico de método.
     *
     * <p>Usado quando apenas alguns parâmetros do método têm @Inject.</p>
     *
     * <p>Exemplo:</p>
     * <pre>{@code
     * @Inject
     * public void init(@Inject UsuariosService service, String mode) {
     *     this.service = service;
     *     this.mode = mode;
     * }
     * }</pre>
     */
    METHOD_PARAMETER
}