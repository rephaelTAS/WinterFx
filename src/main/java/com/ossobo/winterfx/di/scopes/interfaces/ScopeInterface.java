package com.ossobo.winterfx.di.scopes.interfaces;

import java.util.function.Supplier;

/**
 * ScopeInterface v2.0
 *
 * Contrato para escopos do DI.
 *
 * Chave: Class<T> — consistente com getBean(Class<T>).
 * A chave String foi substituída para eliminar conversões desnecessárias
 * e alinhar com o sistema de tipos do container.
 *
 * Responsabilidades:
 * - Armazenar e recuperar instâncias por Class<T>
 * - Criar instâncias via Supplier quando não existirem
 * - Remover instâncias individuais
 * - Destruir o escopo completamente (@PreDestroy nos beans)
 * - Limpar estado temporário (ex: ThreadLocal no fim da requisição)
 *
 * @since 2.0
 */
public interface ScopeInterface {

    /**
     * Obtém ou cria uma instância no escopo.
     *
     * Se a instância já existir para o tipo, retorna a existente.
     * Caso contrário, usa o objectFactory para criar, armazena e retorna.
     *
     * @param type          classe do bean (chave tipada)
     * @param objectFactory callback para criar o bean se necessário
     * @param <T>           tipo do bean
     * @return instância existente ou recém-criada
     */
    <T> T get(Class<T> type, Supplier<T> objectFactory);

    /**
     * Registra um objeto (bean) no escopo, associado ao seu tipo.
     *
     * Útil para registro manual de instâncias já criadas,
     * sem passar pelo Supplier.
     *
     * @param type classe do bean
     * @param bean instância a registar
     * @param <T>  tipo do bean
     */
    <T> void put(Class<T> type, T bean);

    /**
     * Remove um objeto do escopo.
     *
     * @param type classe do bean a remover
     * @param <T>  tipo do bean
     */
    <T> void remove(Class<T> type);

    /**
     * Chamado para destruir o escopo.
     *
     * Deve executar @PreDestroy em todos os beans ativos
     * e libertar todos os recursos.
     * Usado no shutdown do container.
     */
    void destroy();

    /**
     * Chamado para limpar o estado temporário do escopo.
     *
     * Exemplo: limpar o ThreadLocal no fim de uma requisição HTTP,
     * sem destruir as instâncias singleton.
     */
    void clear();
}