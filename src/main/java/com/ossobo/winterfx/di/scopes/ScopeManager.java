package com.ossobo.winterfx.di.scopes;

import com.ossobo.winterfx.di.scopes.enums.ScopeType;
import com.ossobo.winterfx.di.scopes.implementations.SingletonScope;
import com.ossobo.winterfx.di.scopes.implementations.PrototypeScope;
import com.ossobo.winterfx.di.scopes.implementations.ThreadScope;
import com.ossobo.winterfx.di.scopes.interfaces.ScopeInterface;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ScopeManager v2.0
 *
 * Responsabilidade única: gerenciar escopos do DI.
 * Usa ScopeInterface como contrato (chave Class<T>).
 *
 * Métodos para o LifecycleManager:
 * - getSingletonScope()
 * - getThreadScope()
 * - clear()
 *
 * @since 2.0
 */
public final class ScopeManager {

    private final Map<String, ScopeInterface> scopes = new ConcurrentHashMap<>();

    public ScopeManager() {
        registerDefaultScopes();
    }

    private void registerDefaultScopes() {
        registerScope(ScopeType.SINGLETON.getName(), new SingletonScope());
        registerScope(ScopeType.THREAD.getName(), new ThreadScope());
        registerScope(ScopeType.PROTOTYPE.getName(), new PrototypeScope());
    }

    // ===== REGISTRO =====

    public void registerScope(String name, ScopeInterface scope) {
        Objects.requireNonNull(name, "Nome do escopo não pode ser nulo.");
        Objects.requireNonNull(scope, "ScopeInterface não pode ser nulo.");
        scopes.put(name, scope);
    }

    // ===== RESOLUÇÃO =====

    /**
     * Obtém um ScopeInterface pelo nome.
     */
    public ScopeInterface getScopeHandler(String scopeName) {
        return Optional.ofNullable(scopes.get(scopeName))
                .orElseThrow(() -> new IllegalArgumentException("Escopo desconhecido: " + scopeName));
    }

    // ===== ACESSO A ESCOPOS ESPECÍFICOS (para LifecycleManager e InstanceCreator) =====

    /**
     * Obtém o SingletonScope diretamente.
     * Usado para registrar early references e invocar @PreDestroy.
     */
    public SingletonScope getSingletonScope() {
        ScopeInterface scope = scopes.get(ScopeType.SINGLETON.getName());
        if (scope instanceof SingletonScope) {
            return (SingletonScope) scope;
        }
        return null;
    }

    /**
     * Obtém o ThreadScope diretamente.
     * Usado para limpar ThreadLocals no shutdown e entre requisições.
     */
    public ThreadScope getThreadScope() {
        ScopeInterface scope = scopes.get(ScopeType.THREAD.getName());
        if (scope instanceof ThreadScope) {
            return (ThreadScope) scope;
        }
        return null;
    }

    // ===== LIMPEZA =====

    /**
     * Remove todos os escopos registados.
     */
    public void clear() {
        scopes.clear();
    }
}