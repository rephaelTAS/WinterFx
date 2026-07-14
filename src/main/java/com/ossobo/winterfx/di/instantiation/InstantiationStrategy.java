package com.ossobo.winterfx.di.instantiation;

import com.ossobo.winterfx.scanner.models.BeanDefinition;

/**
 * Contrato para estratégias de instanciação de beans.
 *
 * <p>Define a API para a criação de instâncias brutas de beans a partir de suas
 * definições. O {@link InstanceCreator} delega a construção do objeto para a
 * estratégia compatível fornecida pelo {@link InstantiationStrategyManager}.</p>
 *
 * @see ConstructorInstantiationStrategy
 * @see FactoryMethodStrategy
 */
public interface InstantiationStrategy {

    /**
     * Cria uma nova instância bruta do bean com base na sua definição.
     *
     * <p>A instância retornada por este método não deve possuir dependências
     * injetadas; a injeção ocorrerá em uma etapa posterior pelo {@code InstanceCreator}.</p>
     *
     * @param definition Os metadados do bean a ser instanciado.
     * @return A instância recém-criada do bean.
     * @throws Exception Se a reflexão ou a invocação do construtor/método falhar.
     */
    Object instantiate(BeanDefinition definition) throws Exception;

    /**
     * Verifica se esta estratégia é capaz de lidar com a definição de bean fornecida.
     *
     * @param definition A definição do bean a ser avaliada.
     * @return {@code true} se a estratégia for aplicável, {@code false} caso contrário.
     */
    boolean canHandle(BeanDefinition definition);
}