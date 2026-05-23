package com.ossobo.winterfx.di.exceptions;
// Sugestão de pacote para exceções relacionadas à resolução de dependência

/**
 * Exceção lançada quando um bean solicitado não pode ser encontrado
 * no ComponentRegistry ou resolvido pelo DependencyResolver.
 */
public class BeanNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Construtor que aceita o tipo (Class) que não pôde ser encontrado.
     * @param type O tipo do bean (classe) que faltou.
     */
    public BeanNotFoundException(Class<?> type) {
        super("Bean do tipo '" + type.getName() + "' não foi encontrado. "
                + "Verifique se o pacote base foi escaneado e se a classe está anotada/registrada.");
    }

    /**
     * Construtor que aceita um nome (String) ou alias do bean que não pôde ser encontrado.
     * @param name O nome ou alias do bean que faltou.
     */
    public BeanNotFoundException(String name) {
        super("Bean com o nome/alias '" + name + "' não foi encontrado. "
                + "Verifique o ComponentRegistry.");
    }

    /**
     * Construtor completo.
     * @param message Mensagem de erro detalhada.
     * @param cause A exceção original que causou a falha (se houver).
     */
    public BeanNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}