package com.ossobo.winterfx.di.instantiation;

import com.ossobo.winterfx.di.instantiation.strategies.ConstructorInstantiationStrategy;
import com.ossobo.winterfx.di.instantiation.strategies.FactoryMethodStrategy;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.scanner.models.BeanDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Gerenciador responsável por armazenar e selecionar as estratégias de instanciação disponíveis.
 *
 * <p>A seleção da estratégia segue uma ordem de prioridade estrita baseada na lista interna:</p>
 * <ol>
 *   <li>{@link FactoryMethodStrategy} - Para beans criados via métodos fábrica ({@code @Bean}).</li>
 *   <li>{@link ConstructorInstantiationStrategy} - Para beans criados via construtores ({@code @Inject} ou padrão).</li>
 * </ol>
 *
 * <p>Suporta inicialização segura em cenários de "Boot Sequence" (inicialização em etapas),
 * permitindo a criação do gerenciador sem dependências e a injeção tardia do
 * {@link DependencyResolver} através do método {@link #setDependencyResolver(DependencyResolver)}.</p>
 *
 * @version 2.1
 */
public final class InstantiationStrategyManager {

    private final List<InstantiationStrategy> strategies = new ArrayList<>();
    private DependencyResolver dependencyResolver;

    // ============================================================
    // CONSTRUTORES
    // ============================================================

    /**
     * Construtor vazio destinado a sequências de inicialização (Boot Sequence).
     *
     * <p>As estratégias internas só serão instanciadas quando o {@link DependencyResolver}
     * for fornecido posteriormente via {@link #setDependencyResolver(DependencyResolver)}.</p>
     */
    public InstantiationStrategyManager() {
    }

    /**
     * Construtor com injeção imediata de dependências.
     *
     * <p>Inicializa e registra todas as estratégias de instanciação padrão automaticamente.</p>
     *
     * @param dependencyResolver O resolvedor de dependências que será repassado às estratégias.
     */
    public InstantiationStrategyManager(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
        initStrategies();
    }

    // ============================================================
    // INJEÇÃO TARDIA (BootSequence)
    // ============================================================

    /**
     * Define o resolvedor de dependências após a construção do objeto e inicializa as estratégias.
     *
     * <p>Este método é essencial para fluxos de inicialização onde as dependências
     * não estão todas disponíveis no momento da construção do gerenciador.</p>
     *
     * @param dependencyResolver O resolvedor de dependências do contêiner.
     */
    public void setDependencyResolver(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
        initStrategies();
    }

    // ============================================================
    // API PÚBLICA
    // ============================================================

    /**
     * Busca e retorna a primeira estratégia registrada que seja compatível com a definição do bean.
     *
     * @param definition A definição do bean para a qual se deseja uma estratégia.
     * @return Uma estratégia compatível, ou {@code null} se nenhuma estratégia puder manipular a definição.
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

    /**
     * Limpa a lista de estratégias existentes e instancia as estratégias padrão
     * na ordem de prioridade correta.
     */
    private void initStrategies() {
        strategies.clear();
        if (dependencyResolver != null) {
            strategies.add(new FactoryMethodStrategy(dependencyResolver));
            strategies.add(new ConstructorInstantiationStrategy(dependencyResolver));
        }
    }
}