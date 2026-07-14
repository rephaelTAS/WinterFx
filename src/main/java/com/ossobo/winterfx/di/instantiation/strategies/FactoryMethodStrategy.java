package com.ossobo.winterfx.di.instantiation.strategies;

import com.ossobo.winterfx.di.instantiation.InstantiationStrategy;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.scanner.models.BeanDefinition;

import java.lang.reflect.Method;

/**
 * Estratégia de instanciação baseada na invocação de métodos fábrica ({@code @Bean}).
 *
 * <p>Enquanto a {@link ConstructorInstantiationStrategy} lida com a instanciação
 * direta via construtor, esta estratégia é aplicável quando o bean é declarado
 * através de um método anotado com {@code @Bean} em uma classe de configuração.</p>
 *
 * <p>O processo envolve a resolução da instância da classe fábrica (o "bean pai"),
 * a resolução dos argumentos do método fábrica e a subsequente invocação reflexiva
 * do método para obter o novo bean.</p>
 */
public final class FactoryMethodStrategy implements InstantiationStrategy {

    private final DependencyResolver dependencyResolver;

    /**
     * Constrói a estratégia de instanciação por método fábrica.
     *
     * @param dependencyResolver Resolvedor utilizado para obter a instância da classe
     *                           fábrica e as dependências dos parâmetros do método.
     */
    public FactoryMethodStrategy(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    /**
     * Verifica se esta estratégia pode manipular a definição de bean fornecida.
     *
     * <p>Retorna {@code true} apenas se a definição indicar que o bean é criado
     * por um método fábrica.</p>
     *
     * @param definition A definição do bean a ser avaliada.
     * @return {@code true} se a estratégia for aplicável, {@code false} caso contrário.
     */
    @Override
    public boolean canHandle(BeanDefinition definition) {
        return definition.isFactoryMethod();
    }

    /**
     * Cria uma nova instância do bean invocando o método fábrica definido.
     *
     * <p>O método primeiro resolve o bean da classe fábrica a partir do contêiner,
     * em seguida resolve os parâmetros do método e, por fim, o invoca via reflexão.</p>
     *
     * @param definition A definição do bean contendo os metadados do método fábrica e da classe pai.
     * @return A instância do bean retornada pelo método fábrica.
     * @throws Exception Se a classe fábrica não puder ser resolvida, se o método não puder
     *                   ser acessado ou se a invocação falhar.
     */
    @Override
    public Object instantiate(BeanDefinition definition) throws Exception {
        Class<?> factoryClass = definition.getFactoryClass();
        Method factoryMethod = definition.getFactoryMethod();

        Object factoryInstance = dependencyResolver.getBean(factoryClass);
        Object[] args = resolveArguments(factoryMethod);
        factoryMethod.setAccessible(true);

        return factoryMethod.invoke(factoryInstance, args);
    }

    /**
     * Resolve os argumentos necessários para a invocação do método fábrica.
     *
     * @param method O método fábrica cujos parâmetros serão resolvidos.
     * @return Um array de objetos contendo as dependências na ordem exigida pelo método.
     */
    private Object[] resolveArguments(Method method) {
        return java.util.stream.IntStream.range(0, method.getParameterCount())
                .mapToObj(i -> dependencyResolver.resolve(
                        method.getParameters()[i].getParameterizedType(), null))
                .toArray();
    }
}