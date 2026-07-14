package com.ossobo.winterfx.di.instantiation.strategies;

import com.ossobo.winterfx.anotations.Inject;
import com.ossobo.winterfx.di.exceptions.DependencyResolutionException;
import com.ossobo.winterfx.di.instantiation.InstantiationStrategy;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.scanner.models.BeanDefinition;

import java.lang.reflect.Constructor;
import java.util.Arrays;

/**
 * Estratégia de instanciação baseada na invocação de construtores.
 *
 * <p>Esta classe implementa a lógica para criar novas instâncias de beans utilizando
 * reflexão no construtor da classe alvo. A seleção do construtor segue a seguinte
 * ordem de prioridade:</p>
 * <ol>
 *   <li>Construtor anotado com {@code @Inject} (deve haver exatamente um).</li>
 *   <li>Construtor padrão sem argumentos.</li>
 *   <li>Construtor único disponível na classe (caso não haja padrão).</li>
 * </ol>
 *
 * <p>Esta estratégia não é aplicável para beans definidos por métodos fábrica
 * ({@code @Bean}), sendo delegada para {@link FactoryMethodStrategy} nesses casos.</p>
 */
public final class ConstructorInstantiationStrategy implements InstantiationStrategy {

    private final DependencyResolver dependencyResolver;
    private final ReflectionProcessor reflectionProcessor = new ReflectionProcessor();

    /**
     * Constrói a estratégia de instanciação por construtor.
     *
     * @param dependencyResolver Resolvedor utilizado para obter as dependências
     *                           exigidas pelos parâmetros do construtor.
     */
    public ConstructorInstantiationStrategy(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    /**
     * Verifica se esta estratégia pode manipular a definição de bean fornecida.
     *
     * <p>Retorna {@code true} apenas se a definição <b>não</b> for baseada em
     * um método fábrica.</p>
     *
     * @param definition A definição do bean a ser avaliada.
     * @return {@code true} se a estratégia for aplicável, {@code false} caso contrário.
     */
    @Override
    public boolean canHandle(BeanDefinition definition) {
        return !definition.isFactoryMethod();
    }

    /**
     * Cria uma nova instância do bean definido, localizando o construtor adequado,
     * resolvendo suas dependências e invocando-o via reflexão.
     *
     * @param definition A definição do bean contendo os metadados da classe alvo.
     * @return A nova instância do bean.
     * @throws Exception Se o construtor não for encontrado, se houver múltiplos
     *                   construtores {@code @Inject}, ou se a invocação falhar.
     */
    @Override
    public Object instantiate(BeanDefinition definition) throws Exception {
        Class<?> type = definition.getType();
        Constructor<?> constructor = findConstructor(type);
        Object[] args = resolveArguments(constructor);
        return reflectionProcessor.instantiate(constructor, args);
    }

    /**
     * Localiza o construtor a ser utilizado para a instanciação com base nas regras de prioridade.
     *
     * @param type A classe do bean a ser instanciado.
     * @return O construtor selecionado.
     * @throws DependencyResolutionException Se houver múltiplos construtores anotados com {@code @Inject}
     *                                       ou se nenhum construtor válido for encontrado.
     */
    private Constructor<?> findConstructor(Class<?> type) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();

        var annotated = Arrays.stream(constructors)
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .toList();

        if (annotated.size() == 1) return annotated.get(0);
        if (annotated.size() > 1) {
            throw new DependencyResolutionException(
                    "Múltiplos @Inject em: " + type.getName());
        }

        return Arrays.stream(constructors)
                .filter(c -> c.getParameterCount() == 0)
                .findFirst()
                .orElseGet(() -> {
                    if (constructors.length == 1) return constructors[0];
                    throw new DependencyResolutionException(
                            "Nenhum construtor padrão em: " + type.getName());
                });
    }

    /**
     * Resolve os argumentos necessários para a invocação do construtor selecionado.
     *
     * @param constructor O construtor cujos parâmetros serão resolvidos.
     * @return Um array de objetos contendo as dependências na ordem exigida.
     */
    private Object[] resolveArguments(Constructor<?> constructor) {
        return Arrays.stream(constructor.getParameters())
                .map(p -> dependencyResolver.resolve(p.getParameterizedType(), null))
                .toArray();
    }
}