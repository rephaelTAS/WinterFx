package com.ossobo.winterfx.di.aot;


import com.ossobo.winterfx.di.resolver.DependencyResolver;

/**
 * Interface que define o contrato para factories de instâncias geradas AOT (Ahead-Of-Time).
 * Essa interface substitui a reflexão lenta do InstanceCreator.
 *
 * T - O tipo do bean a ser criado.
 */
@FunctionalInterface
public interface InstanceFactory<T> {

    /**
     * Cria e retorna uma nova instância do bean T, resolvendo suas dependências.
     *
     * @param resolver O DependencyResolver do Container para resolver as dependências do construtor.
     * @return Uma nova instância do tipo T.
     */
    T create(DependencyResolver resolver);
}