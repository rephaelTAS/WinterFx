package com.ossobo.winterfx.di.scopes.enums;

/**
 * ScopeType v2.0
 *
 * Tipos de escopo suportados pelo container DI.
 *
 * - SINGLETON: uma instância por container
 * - PROTOTYPE: nova instância a cada pedido
 * - THREAD: uma instância por thread (ideal para requisições HTTP)
 *
 * Nota: "REQUEST" foi renomeado para "THREAD" porque o escopo é vinculado
 * à thread, não ao conceito HTTP de request. O filtro de requisição
 * limpa o ThreadLocal após cada request.
 *
 * @since 2.0
 */
public enum ScopeType {

    SINGLETON,
    PROTOTYPE,
    THREAD;

    /**
     * Converte uma string para o ScopeType correspondente.
     *
     * Aceita "request" como alias para THREAD (retrocompatibilidade).
     * Case-insensitive.
     * Se não reconhecer, retorna SINGLETON como padrão seguro.
     *
     * @param scope nome do escopo (case-insensitive)
     * @return ScopeType correspondente, ou SINGLETON se desconhecido
     */
    public static ScopeType fromString(String scope) {
        if (scope == null || scope.isBlank()) {
            return SINGLETON;
        }

        return switch (scope.toLowerCase()) {
            case "singleton"                -> SINGLETON;
            case "prototype"                -> PROTOTYPE;
            case "thread", "request"        -> THREAD;
            default -> {
                System.err.println("Escopo desconhecido: " + scope + ". Usando SINGLETON.");
                yield SINGLETON;
            }
        };
    }

    /**
     * Retorna o nome do escopo em minúsculas.
     * Consistente com o formato esperado pelo ScopeManager.
     */
    public String getName() {
        return name().toLowerCase();
    }
}