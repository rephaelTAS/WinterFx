package com.ossobo.winterfx.di.aot;

import com.ossobo.winterfx.di.resolver.DependencyResolver;

/**
 * Contrato para fábricas de instâncias geradas em tempo de compilação (Ahead-Of-Time).
 *
 * <p>Esta interface substitui a necessidade de reflexão em tempo de execução (RTTI)
 * durante a criação de instâncias, permitindo que o mecanismo de injeção de dependências
 * instancie beans de forma otimizada quando as factories AOT estão disponíveis.</p>
 *
 * @param <T> O tipo do bean a ser criado pela fábrica.
 */
@FunctionalInterface
public interface InstanceFactory<T> {

    /**
     * Cria e retorna uma nova instância do tipo {@code T}, resolvendo suas dependências
     * através do resolvedor fornecido.
     *
     * @param resolver O resolvedor de dependências do contêiner, utilizado para fornecer
     *                 as instâncias exigidas pelo construtor do bean.
     * @return Uma nova instância totalmente inicializada do tipo {@code T}.
     */
    T create(DependencyResolver resolver);
}