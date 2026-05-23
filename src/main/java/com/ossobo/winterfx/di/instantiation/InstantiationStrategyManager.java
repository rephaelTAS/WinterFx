package com.ossobo.winterfx.di.instantiation;

import com.ossobo.winterfx.di.instantiation.strategies.ConstructorInstantiationStrategy;
import com.ossobo.winterfx.di.instantiation.strategies.FactoryMethodStrategy;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.di.scanner.models.BeanDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * InstantiationStrategyManager v2.1
 *
 * Gerencia as estratégias de instanciação.
 * Ordem de prioridade:
 * 1. FactoryMethodStrategy (@Configuration + @Bean)
 * 2. ConstructorInstantiationStrategy (@Inject ou padrão)
 *
 * Suporta inicialização segura via BootSequence:
 * - Construtor vazio para criação sem dependências
 * - setDependencyResolver() para injeção tardia
 *
 * @version v2.1 (18/05/2026)
 */
public final class InstantiationStrategyManager {

    private final List<InstantiationStrategy> strategies = new ArrayList<>();
    private DependencyResolver dependencyResolver;

    // ============================================================
    // CONSTRUTORES
    // ============================================================

    /**
     * Construtor vazio — para BootSequence.
     * Estratégias são criadas apenas quando o resolver é injetado.
     */
    public InstantiationStrategyManager() {
        // vazio — resolver será injetado depois via setter
    }

    /**
     * Construtor com dependência — compatível com código existente.
     */
    public InstantiationStrategyManager(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
        initStrategies();
    }

    // ============================================================
    // INJEÇÃO TARDIA (BootSequence)
    // ============================================================

    /**
     * Define o DependencyResolver após construção.
     * Cria as estratégias de instanciação com o resolver real.
     * Usado pelo BootSequence na fase de INJEÇÃO.
     *
     * @param dependencyResolver Resolver de dependências do container
     */
    public void setDependencyResolver(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
        initStrategies();
    }

    // ============================================================
    // API PÚBLICA
    // ============================================================

    /**
     * Encontra a estratégia adequada para uma definição de bean.
     *
     * @param definition Definição do bean a instanciar
     * @return Estratégia compatível, ou null se nenhuma servir
     */
    public InstantiationStrategy getStrategy(BeanDefinition definition) {
        return strategies.stream()
                .filter(s -> s.canHandle(definition))
                .findFirst()
                .orElse(null);
    }

    // ============================================================
    // INTERNO
    // ============================================================

    private void initStrategies() {
        strategies.clear();
        if (dependencyResolver != null) {
            strategies.add(new FactoryMethodStrategy(dependencyResolver));
            strategies.add(new ConstructorInstantiationStrategy(dependencyResolver));
        }
    }
}