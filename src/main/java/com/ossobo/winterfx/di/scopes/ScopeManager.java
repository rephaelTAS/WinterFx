package com.ossobo.winterfx.di.scopes;

import com.ossobo.winterfx.scanner.enums.ScopeType;
import com.ossobo.winterfx.di.scopes.implementations.SingletonScope;
import com.ossobo.winterfx.di.scopes.implementations.PrototypeScope;
import com.ossobo.winterfx.di.scopes.implementations.ThreadScope;
import com.ossobo.winterfx.di.scopes.interfaces.ScopeInterface;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador responsável pelo registro e resolução dos escopos de instância do contêiner de DI.
 *
 * <p>Os escopos determinam o ciclo de vida e a estratégia de reutilização das instâncias
 * dos beans gerenciados. Esta classe atua como um registro centralizado, mapeando nomes
 * lógicos de escopos para suas implementações concretas ({@link ScopeInterface}).</p>
 *
 * <p>Durante a inicialização, o gerenciador registra automaticamente os três escopos padrão
 * do framework:</p>
 * <ul>
 *     <li><b>Singleton:</b> Uma única instância compartilhada por todo o contêiner.</li>
 *     <li><b>Thread:</b> Uma instância por thread, armazenada em {@code ThreadLocal}.</li>
 *     <li><b>Prototype:</b> Uma nova instância a cada solicitação.</li>
 * </ul>
 *
 * @since 2.0
 */
public final class ScopeManager {

    private final Map<String, ScopeInterface> scopes = new ConcurrentHashMap<>();

    /**
     * Constrói o gerenciador e registra os escopos padrão do framework.
     */
    public ScopeManager() {
        registerDefaultScopes();
    }

    /**
     * Instancia e registra os escopos nativos ({@code Singleton}, {@code Thread}, {@code Prototype}).
     */
    private void registerDefaultScopes() {
        registerScope(ScopeType.SINGLETON.getName(), new SingletonScope());
        registerScope(ScopeType.THREAD.getName(), new ThreadScope());
        registerScope(ScopeType.PROTOTYPE.getName(), new PrototypeScope());
    }

    // ===== REGISTRO =====

    /**
     * Registra uma nova implementação de escopo no contêiner.
     *
     * <p>Permite a extensão do framework com escopos personalizados (ex: escopo de requisição HTTP).</p>
     *
     * @param name  O nome lógico identificador do escopo.
     * @param scope A implementação do contrato de escopo.
     * @throws NullPointerException se o nome ou a implementação forem nulos.
     */
    public void registerScope(String name, ScopeInterface scope) {
        Objects.requireNonNull(name, "Nome do escopo não pode ser nulo.");
        Objects.requireNonNull(scope, "ScopeInterface não pode ser nulo.");
        scopes.put(name, scope);
    }

    // ===== RESOLUÇÃO =====

    /**
     * Recupera a implementação de escopo associada ao nome fornecido.
     *
     * @param scopeName O nome lógico do escopo (ex: "singleton", "prototype").
     * @return A implementação do escopo correspondente.
     * @throws IllegalArgumentException Se nenhum escopo estiver registrado com o nome fornecido.
     */
    public ScopeInterface getScopeHandler(String scopeName) {
        return Optional.ofNullable(scopes.get(scopeName))
                .orElseThrow(() -> new IllegalArgumentException("Escopo desconhecido: " + scopeName));
    }

    // ===== ACESSO A ESCOPOS ESPECÍFICOS (para LifecycleManager e InstanceCreator) =====

    /**
     * Recupera o escopo de {@code Singleton} de forma tipada.
     *
     * <p>Utilizado internamente para registrar referências antecipadas (early references)
     * durante a resolução de dependências circulares e para invocar métodos de destruição
     * durante o shutdown do contêiner.</p>
     *
     * @return A instância do {@link SingletonScope}, ou {@code null} se não estiver registrado.
     */
    public SingletonScope getSingletonScope() {
        ScopeInterface scope = scopes.get(ScopeType.SINGLETON.getName());
        if (scope instanceof SingletonScope) {
            return (SingletonScope) scope;
        }
        return null;
    }

    /**
     * Recupera o escopo de {@code Thread} de forma tipada.
     *
     * <p>Utilizado internamente pelo gerenciador de ciclo de vida para limpar os
     * {@code ThreadLocals} associados durante o shutdown ou entre requisições.</p>
     *
     * @return A instância do {@link ThreadScope}, ou {@code null} se não estiver registrado.
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
     * Remove todos os escopos registrados, incluindo os escopos padrão.
     */
    public void clear() {
        scopes.clear();
    }
}