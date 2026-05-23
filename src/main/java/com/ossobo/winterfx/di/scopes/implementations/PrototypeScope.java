package com.ossobo.winterfx.di.scopes.implementations;

import com.ossobo.winterfx.di.scopes.interfaces.ScopeInterface;

import java.util.function.Supplier;

/**
 * PrototypeScope v2.0
 *
 * Implementação do escopo Prototype.
 *
 * Garante que uma nova instância seja criada a cada pedido.
 * Não armazena instâncias — o container ou o chamador é responsável
 * pelo ciclo de vida do objeto retornado.
 *
 * Métodos não aplicáveis (put, remove, destroy, clear) têm implementações
 * vazias, pois este escopo não mantém estado.
 *
 * @since 2.0
 */
public final class PrototypeScope implements ScopeInterface {

    /**
     * Construtor padrão. Sem estado interno.
     */
    public PrototypeScope() {
        // Sem inicialização necessária
    }

    /**
     * Cria uma nova instância a cada chamada.
     *
     * @param type          classe do bean
     * @param objectFactory factory que cria a instância
     * @param <T>           tipo do bean
     * @return nova instância criada pelo factory
     */
    @Override
    public <T> T get(Class<T> type, Supplier<T> objectFactory) {
        return objectFactory.get();
    }

    /**
     * Não armazena — implementação vazia.
     */
    @Override
    public <T> void put(Class<T> type, T bean) {
        // Prototype não retém instâncias
    }

    /**
     * Não armazena — implementação vazia.
     */
    @Override
    public <T> void remove(Class<T> type) {
        // Prototype não retém instâncias
    }

    /**
     * Nenhum estado para destruir.
     */
    @Override
    public void destroy() {
        // Sem recursos para libertar
    }

    /**
     * Nenhum estado temporário para limpar.
     */
    @Override
    public void clear() {
        // Sem cache para limpar
    }
}