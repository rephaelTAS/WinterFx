package com.ossobo.winterfx.di;

import com.ossobo.winterfx.di.configuration.ConfigurationManager;
import com.ossobo.winterfx.di.injection.InjectionManager;
import com.ossobo.winterfx.di.instantiation.InstanceCreator;
import com.ossobo.winterfx.di.instantiation.InstantiationStrategyManager;
import com.ossobo.winterfx.di.lifecycle.LifecycleManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.di.resolver.methods.CircularDependencyDetector;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.scanner.ReflectionScanner;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;

/**
 * Orquestrador responsável pela inicialização segura e sequencial dos componentes do DI Container.
 *
 * <p>Para evitar falhas de resolução circular durante a construção do próprio contêiner,
 * este classe divide a inicialização em duas fases estritas:</p>
 * <ul>
 *   <li><b>FASE 1 — Nascimento:</b> Instancia todas as classes de infraestrutura utilizando
 *       exclusivamente construtores vazios.</li>
 *   <li><b>FASE 2 — Injeção:</b> Conecta as dependências entre os componentes recém-criados
 *       utilizando exclusivamente métodos setters.</li>
 * </ul>
 *
 * <p>Este design garante que não haja tentativa de resolver beans de negócio durante a
 * montagem da estrutura interna do contêiner.</p>
 *
 * @version 4.0
 */
public final class BootSequence {

    // ===== NÍVEL 0 =====
    private final ScopeManager scopeManager;
    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final LifecycleEventPublisher eventPublisher;
    private final CircularDependencyDetector circularDetector;
    private final ConfigurationManager configurationManager;
    private final BeanRegistry beanRegistry;
    private final LifecycleManager lifecycleManager;

    // ===== NÍVEL 1 =====
    private InjectionManager injectionManager;
    private InstanceCreator instanceCreator;
    private InstantiationStrategyManager strategyManager;
    private DependencyResolver dependencyResolver;

    /**
     * Constrói a sequência de inicialização fornecendo o registro de beans.
     *
     * <p>Executa imediatamente a Fase 1 (Nascimento), criando todas as instâncias
     * de infraestrutura necessárias para o funcionamento do DI.</p>
     *
     * @param beanRegistry O registro de definições de beans escaneados pelo framework.
     */
    public BootSequence(BeanRegistry beanRegistry) {
        this.beanRegistry = beanRegistry;

        this.scopeManager = new ScopeManager();
        ReflectionScanner scanner = new ReflectionScanner();
        this.reflectionCache = new ReflectionCache(scanner);
        this.reflectionProcessor = new ReflectionProcessor();
        this.eventPublisher = new LifecycleEventPublisher();
        this.circularDetector = new CircularDependencyDetector();
        this.configurationManager = new ConfigurationManager();
        this.configurationManager.loadConfiguration();
        this.lifecycleManager = new LifecycleManager(
                reflectionCache, reflectionProcessor, scopeManager, eventPublisher);

        this.strategyManager = new InstantiationStrategyManager();
        this.injectionManager = new InjectionManager();
        this.instanceCreator = new InstanceCreator();
        this.dependencyResolver = new DependencyResolver();
    }

    /**
     * Executa a Fase 2 (Injeção), conectando todos os componentes através de seus setters.
     */
    private void inject() {
        dependencyResolver.setComponentRegistry(beanRegistry);
        dependencyResolver.setScopeManager(scopeManager);
        dependencyResolver.setInstanceCreator(instanceCreator);
        dependencyResolver.setLifecycleManager(lifecycleManager);
        dependencyResolver.setEventPublisher(eventPublisher);
        dependencyResolver.setCircularDependencyDetector(circularDetector);

        injectionManager.setReflectionCache(reflectionCache);
        injectionManager.setReflectionProcessor(reflectionProcessor);
        injectionManager.setDependencyResolver(dependencyResolver);
        injectionManager.setConfigurationManager(configurationManager);
        injectionManager.setEventPublisher(eventPublisher);

        instanceCreator.setDependencyResolver(dependencyResolver);
        instanceCreator.setInjectionManager(injectionManager);
        instanceCreator.setLifecycleManager(lifecycleManager);
        instanceCreator.setScopeManager(scopeManager);
        instanceCreator.setComponentRegistry(beanRegistry);
        instanceCreator.setEventPublisher(eventPublisher);
        instanceCreator.setStrategyManager(strategyManager);

        strategyManager.setDependencyResolver(dependencyResolver);

        injectionManager.initCoreInjectors();
    }

    /**
     * Valida se todos os componentes cruciais foram instanciados e injetados corretamente.
     *
     * @throws IllegalStateException Se um ou mais componentes obrigatórios estiverem nulos.
     */
    private void validate() {
        StringBuilder erros = new StringBuilder();
        checkNotNull(dependencyResolver, "dependencyResolver", erros);
        checkNotNull(injectionManager, "injectionManager", erros);
        checkNotNull(instanceCreator, "instanceCreator", erros);
        checkNotNull(strategyManager, "strategyManager", erros);
        checkNotNull(beanRegistry, "beanRegistry", erros);
        checkNotNull(scopeManager, "scopeManager", erros);
        checkNotNull(lifecycleManager, "lifecycleManager", erros);
        checkNotNull(reflectionCache, "reflectionCache", erros);
        checkNotNull(configurationManager, "configurationManager", erros);
        checkNotNull(eventPublisher, "eventPublisher", erros);
        checkNotNull(circularDetector, "circularDetector", erros);

        if (erros.length() > 0) {
            throw new IllegalStateException(
                    "BootSequence — Componentes não inicializados:\n" + erros);
        }
    }

    /**
     * Método auxiliar que verifica se um objeto é nulo e registra o erro em um buffer.
     *
     * @param obj   O objeto a ser verificado.
     * @param nome  O nome lógico do objeto para fins de mensagem de erro.
     * @param erros O buffer onde a mensagem de erro será acumulada, se aplicável.
     */
    private void checkNotNull(Object obj, String nome, StringBuilder erros) {
        if (obj == null) {
            erros.append("   ").append(nome).append(" está NULL\n");
        }
    }

    /**
     * Executa o processo completo de bootstrap do contêiner de DI.
     *
     * <p>Invoca a injeção de dependências entre os componentes internos, valida o estado
     * resultante, dispara o evento de inicialização do ciclo de vida e retorna um objeto
     * imutável contendo todas as referências necessárias para o {@link DiContainer}.</p>
     *
     * @return Um {@link BootResult} contendo todos os componentes inicializados e conectados.
     * @throws IllegalStateException Se a validação dos componentes falhar.
     */
    public BootResult boot() {
        inject();
        validate();
        lifecycleManager.initialize();
        return new BootResult(
                dependencyResolver, injectionManager, instanceCreator,
                strategyManager, beanRegistry, scopeManager, lifecycleManager,
                configurationManager, reflectionCache, reflectionProcessor,
                eventPublisher, circularDetector
        );
    }

    /**
     * Record imutável que agrupa o resultado bem-sucedido do processo de bootstrap.
     *
     * @param dependencyResolver   Resolvedor de dependências configurado.
     * @param injectionManager     Gerenciador de injeção configurado.
     * @param instanceCreator      Criador de instâncias configurado.
     * @param strategyManager      Gerenciador de estratégias de instanciação.
     * @param beanRegistry         Registro de beans do framework.
     * @param scopeManager         Gerenciador de escopos.
     * @param lifecycleManager     Gerenciador de ciclo de vida.
     * @param configurationManager Gerenciador de configurações.
     * @param reflectionCache      Cache de reflexão.
     * @param reflectionProcessor  Processador de reflexão.
     * @param eventPublisher       Publicador de eventos de ciclo de vida.
     * @param circularDetector     Detector de dependências circulares.
     */
    public record BootResult(
            DependencyResolver dependencyResolver,
            InjectionManager injectionManager,
            InstanceCreator instanceCreator,
            InstantiationStrategyManager strategyManager,
            BeanRegistry beanRegistry,
            ScopeManager scopeManager,
            LifecycleManager lifecycleManager,
            ConfigurationManager configurationManager,
            ReflectionCache reflectionCache,
            ReflectionProcessor reflectionProcessor,
            LifecycleEventPublisher eventPublisher,
            CircularDependencyDetector circularDetector
    ) {}
}