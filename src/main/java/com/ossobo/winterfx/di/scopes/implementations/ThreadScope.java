package com.ossobo.winterfx.di.scopes.implementations;

import com.ossobo.winterfx.di.scopes.interfaces.ScopeInterface;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ThreadScope v2.0
 *
 * Implementação do escopo Thread.
 *
 * Garante que cada instância de um bean com este escopo seja única
 * para a thread que a solicitou. Usa ThreadLocal para isolamento.
 *
 * Ideal para:
 * - Requisições HTTP (uma instância por request)
 * - Tarefas assíncronas com estado por thread
 * - Contextos onde o isolamento entre threads é necessário
 *
 * A chave foi migrada de String para Class<T>, alinhando com a
 * ScopeInterface v2.0 e eliminando conversões desnecessárias.
 *
 * @since 2.0
 */
public final class ThreadScope implements ScopeInterface {

    private static final Logger LOGGER = Logger.getLogger(ThreadScope.class.getName());

    /**
     * ThreadLocal que armazena um mapa de instâncias para cada thread.
     * Chave: Class<?> (tipo do bean).
     * Cada thread tem seu próprio ConcurrentHashMap.
     */
    private final ThreadLocal<Map<Class<?>, Object>> threadInstances =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    public ThreadScope() {
        // Sem inicialização necessária
    }

    // ===== ScopeInterface =====

    /**
     * Obtém ou cria uma instância para a thread atual.
     *
     * Se a instância já existir para este tipo nesta thread, retorna a existente.
     * Caso contrário, usa o objectFactory para criar, armazena e retorna.
     *
     * @param type          classe do bean (chave tipada)
     * @param objectFactory factory para criar se necessário
     * @param <T>           tipo do bean
     * @return instância para esta thread
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type, Supplier<T> objectFactory) {
        return (T) threadInstances.get()
                .computeIfAbsent(type, k -> objectFactory.get());
    }

    /**
     * Registra uma instância manualmente para a thread atual.
     *
     * @param type classe do bean
     * @param bean instância a registar
     * @param <T>  tipo do bean
     */
    @Override
    public <T> void put(Class<T> type, T bean) {
        threadInstances.get().put(type, bean);
        LOGGER.log(Level.FINE, "ThreadScope: Bean {0} registado na thread {1}.",
                new Object[]{type.getName(), Thread.currentThread().getName()});
    }

    /**
     * Remove uma instância da thread atual.
     *
     * @param type classe do bean a remover
     * @param <T>  tipo do bean
     */
    @Override
    public <T> void remove(Class<T> type) {
        threadInstances.get().remove(type);
        LOGGER.log(Level.FINE, "ThreadScope: Bean {0} removido da thread {1}.",
                new Object[]{type.getName(), Thread.currentThread().getName()});
    }

    /**
     * Limpa o cache de instâncias da thread atual.
     *
     * Chamado no fim de uma requisição HTTP (via filtro) ou
     * quando a thread terminou seu trabalho.
     * Remove o mapa inteiro do ThreadLocal para esta thread.
     */
    @Override
    public void clear() {
        threadInstances.remove();
        LOGGER.log(Level.FINE, "ThreadScope: Cache limpo para thread {0}.",
                Thread.currentThread().getName());
    }

    /**
     * Destrói o escopo para a thread atual.
     *
     * Mesmo comportamento que clear() para este escopo,
     * já que cada thread gere suas próprias instâncias.
     */
    @Override
    public void destroy() {
        clear();
        LOGGER.log(Level.FINE, "ThreadScope: Escopo destruído para thread {0}.",
                Thread.currentThread().getName());
    }

    // ===== MÉTODOS DE UTILIDADE (para ScopeManager e LifecycleManager) =====

    /**
     * Limpa o escopo da thread atual e retorna as instâncias que estavam ativas.
     *
     * Usado pelo ScopeManager para obter as instâncias e executar
     * @PreDestroy em cada uma delas antes de descartar.
     *
     * @return mapa de instâncias que estavam ativas nesta thread
     */
    public Map<Class<?>, Object> clearAndGetInstances() {
        Map<Class<?>, Object> instances = threadInstances.get();
        threadInstances.remove();
        LOGGER.log(Level.FINE, "ThreadScope: {0} instâncias recolhidas para @PreDestroy na thread {1}.",
                new Object[]{instances.size(), Thread.currentThread().getName()});
        return instances;
    }

    /**
     * Alerta sobre a limitação de limpar ThreadLocals de outras threads.
     *
     * Chamado no shutdown do container.
     * Como Java não permite aceder ao ThreadLocal de outras threads,
     * apenas regista um aviso. Cabe ao pool de threads ou ao filtro
     * de requisições limpar suas próprias ThreadLocals.
     */
    public void clearAllThreads() {
        LOGGER.log(Level.WARNING,
                "ThreadScope: clearAllThreads() chamado. " +
                        "O DI Container está a desligar, mas apenas a thread atual pode ser limpa. " +
                        "Certifique-se de que o pool de threads ou filtro de requisições " +
                        "limpa os seus próprios ThreadLocals.");
    }
}