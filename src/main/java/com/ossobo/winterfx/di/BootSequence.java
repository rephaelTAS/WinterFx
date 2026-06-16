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
import com.ossobo.winterfx.scanner.BeanMetadataExtractor;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;
import com.ossobo.winterfx.scanner.ReflectionScanner;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.view.loader.FXMLService;

/**
 * BootSequence v3.0 — Orquestrador de inicialização segura do DiContainer.
 *
 * <p><b>Mudanças v2.0 → v3.0:</b></p>
 * <ul>
 *   <li>🗑️ ComponentScanner removido — scan é feito externamente pelo ScannerEngine</li>
 *   <li>🔄 BeanRegistry recebido no construtor (já populado)</li>
 *   <li>🗑️ packages removido — não é mais responsabilidade do BootSequence</li>
 *   <li>🔄 LifecycleManager simplificado — recebe BeanRegistry + ScopeManager</li>
 *   <li>🔄 scan() agora só inicializa LifecycleManager (sem ClassGraph)</li>
 *   <li>🗑️ componentScanner removido do BootResult</li>
 * </ul>
 *
 * <p><b>Padrão de 2 fases (mantido):</b></p>
 * <ul>
 *   <li>FASE 1 — NASCIMENTO: Constrói todas as classes com construtor vazio</li>
 *   <li>FASE 2 — INJEÇÃO:   Conecta dependências exclusivamente via setters</li>
 * </ul>
 *
 * @version v3.0 (31/05/2026)
 */
public final class BootSequence {

    // ===== COMPONENTES NÍVEL 0 (sem dependências) =====
    private final ScopeManager scopeManager;
    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final LifecycleEventPublisher eventPublisher;
    private final CircularDependencyDetector circularDetector;
    private final ConfigurationManager configurationManager;
    private final BeanRegistry beanRegistry;
    private final LifecycleManager lifecycleManager;
    private final BeanMetadataExtractor metadataExtractor;
    private final ReflectionScanner reflectionScanner;

    // ===== COMPONENTES NÍVEL 1 (recebem dependências via setters) =====
    private InjectionManager injectionManager;
    private InstanceCreator instanceCreator;
    private InstantiationStrategyManager strategyManager;
    private DependencyResolver dependencyResolver;

    /**
     * 🔄 FASE 1 — NASCIMENTO: Constrói todas as classes.
     *
     * <p><b>BeanRegistry já vem populado do ScannerEngine.</b>
     * BootSequence NÃO faz mais scan de classpath.</p>
     *
     * @param beanRegistry registry JÁ populado pelo ScannerEngine
     */
    public BootSequence(BeanRegistry beanRegistry) {
        this.beanRegistry = beanRegistry;

        // ============================================================
        // NÍVEL 0: Infraestrutura — não dependem de ninguém
        // ============================================================
        this.scopeManager = new ScopeManager();
        this.reflectionCache = new ReflectionCache(new ReflectionScanner());
        this.reflectionProcessor = new ReflectionProcessor();
        this.eventPublisher = new LifecycleEventPublisher();
        this.circularDetector = new CircularDependencyDetector();
        this.configurationManager = new ConfigurationManager();
        this.lifecycleManager = new LifecycleManager(
                reflectionCache, reflectionProcessor, scopeManager, eventPublisher);
        this.reflectionScanner = new ReflectionScanner();
        this.metadataExtractor = new BeanMetadataExtractor(reflectionScanner);

        // ============================================================
        // NÍVEL 1: Managers — construtores vazios, 100% seguros
        // ============================================================
        this.strategyManager = new InstantiationStrategyManager();
        this.injectionManager = new InjectionManager();
        this.instanceCreator = new InstanceCreator();
        this.dependencyResolver = new DependencyResolver();
    }

    /**
     * FASE 2 — INJEÇÃO: Conecta todas as dependências via setters.
     */
    private void inject() {
        // ============================================================
        // dependencyResolver
        // ============================================================
        dependencyResolver.setComponentRegistry(beanRegistry);
        dependencyResolver.setScopeManager(scopeManager);
        dependencyResolver.setInstanceCreator(instanceCreator);
        dependencyResolver.setLifecycleManager(lifecycleManager);
        dependencyResolver.setEventPublisher(eventPublisher);
        dependencyResolver.setCircularDependencyDetector(circularDetector);
        dependencyResolver.setMetadataExtractor(metadataExtractor);

        // ============================================================
        // injectionManager
        // ============================================================
        injectionManager.setReflectionCache(reflectionCache);
        injectionManager.setReflectionProcessor(reflectionProcessor);
        injectionManager.setDependencyResolver(dependencyResolver);
        injectionManager.setConfigurationManager(configurationManager);
        injectionManager.setEventPublisher(eventPublisher);

        // ============================================================
        // instanceCreator
        // ============================================================
        instanceCreator.setDependencyResolver(dependencyResolver);
        instanceCreator.setInjectionManager(injectionManager);
        instanceCreator.setLifecycleManager(lifecycleManager);
        instanceCreator.setScopeManager(scopeManager);
        instanceCreator.setComponentRegistry(beanRegistry);
        instanceCreator.setEventPublisher(eventPublisher);
        instanceCreator.setStrategyManager(strategyManager);

        // ============================================================
        // strategyManager
        // ============================================================
        strategyManager.setDependencyResolver(dependencyResolver);

        // ============================================================
        // 🆕 INICIALIZA INJECTORS (APÓS TODOS OS SETTERS!)
        // ============================================================
        injectionManager.initCoreInjectors();
    }

    /**
     * Valida que nenhum componente crítico ficou null.
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
                    "❌ BootSequence — Componentes não inicializados:\n" + erros);
        }
    }

    private void checkNotNull(Object obj, String nome, StringBuilder erros) {
        if (obj == null) {
            erros.append("   ❌ ").append(nome).append(" está NULL\n");
        }
    }

    /**
     * 🔄 Inicializa o LifecycleManager.
     *
     * <p>NÃO faz mais scan de classpath — os beans já estão no BeanRegistry.</p>
     */
    private void scan() {
        lifecycleManager.initialize();
    }

    /**
     * Executa a sequência completa: NASCIMENTO → INJEÇÃO → VALIDAÇÃO → CICLO DE VIDA.
     */
    public BootResult boot() {
        inject();
        validate();
        scan();
        return new BootResult(
                dependencyResolver,
                injectionManager,
                instanceCreator,
                strategyManager,
                beanRegistry,
                scopeManager,
                lifecycleManager,
                metadataExtractor
        );
    }

    /**
     * 🔄 Resultado do boot — componentScanner removido.
     */
    public record BootResult(
            DependencyResolver dependencyResolver,
            InjectionManager injectionManager,
            InstanceCreator instanceCreator,
            InstantiationStrategyManager strategyManager,
            BeanRegistry beanRegistry,
            ScopeManager scopeManager,
            LifecycleManager lifecycleManager,
            BeanMetadataExtractor metadataExtractor
    ) {
        public int getBeanCount() {
            return beanRegistry.getBeanNames().size();
        }
    }
}