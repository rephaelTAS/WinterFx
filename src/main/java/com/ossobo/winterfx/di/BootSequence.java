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

import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final Logger LOGGER = Logger.getLogger(BootSequence.class.getName());

    // ===== COMPONENTES NÍVEL 0 (sem dependências) =====
    private final ScopeManager scopeManager;
    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final LifecycleEventPublisher eventPublisher;
    private final CircularDependencyDetector circularDetector;
    private final ConfigurationManager configurationManager;
    private final BeanRegistry beanRegistry;             // 🔄 Recebido de fora (já populado)
    // 🗑️ private final ComponentScanner componentScanner;  // REMOVIDO
    private final LifecycleManager lifecycleManager;
    private final BeanMetadataExtractor metadataExtractor;
    private final ReflectionScanner reflectionScanner;

    // ===== COMPONENTES NÍVEL 1 (recebem dependências via setters) =====
    private InjectionManager injectionManager;
    private InstanceCreator instanceCreator;
    private InstantiationStrategyManager strategyManager;
    private DependencyResolver dependencyResolver;

    // 🗑️ private final String[] packages;  // REMOVIDO

    /**
     * 🔄 FASE 1 — NASCIMENTO: Constrói todas as classes.
     *
     * <p><b>BeanRegistry já vem populado do ScannerEngine.</b>
     * BootSequence NÃO faz mais scan de classpath.</p>
     *
     * @param beanRegistry registry JÁ populado pelo ScannerEngine
     */
    public BootSequence(BeanRegistry beanRegistry) {
        this.beanRegistry = beanRegistry;  // 🔄 Recebido, não criado
        LOGGER.log(Level.INFO, "🔧 BootSequence v3.0 FASE 1 — NASCIMENTO: {0} beans pré-registados.",
                beanRegistry.getBeanNames().size());

        // ============================================================
        // NÍVEL 0: Infraestrutura — não dependem de ninguém
        // ============================================================
        this.scopeManager = new ScopeManager();
        this.reflectionCache = new ReflectionCache(new ReflectionScanner());
        this.reflectionProcessor = new ReflectionProcessor();
        this.eventPublisher = new LifecycleEventPublisher();
        this.circularDetector = new CircularDependencyDetector();
        this.configurationManager = new ConfigurationManager();
        // 🗑️ this.componentScanner = new ComponentScanner(beanRegistry, packages);
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

        LOGGER.log(Level.INFO, "   ✅ 12 componentes construídos (4 vazios).");  // 🔄 13 → 12
    }

    /**
     * FASE 2 — INJEÇÃO: Conecta todas as dependências via setters.
     */
    private void inject() {
        LOGGER.log(Level.INFO, "🔗 BootSequence FASE 2 — INJEÇÃO: Conectando dependências...");

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
        LOGGER.log(Level.FINE, "   ├─ DependencyResolver ← 7 dependências");

        // ============================================================
        // injectionManager
        // ============================================================
        injectionManager.setReflectionCache(reflectionCache);
        injectionManager.setReflectionProcessor(reflectionProcessor);
        injectionManager.setDependencyResolver(dependencyResolver);
        injectionManager.setConfigurationManager(configurationManager);
        injectionManager.setEventPublisher(eventPublisher);
        LOGGER.log(Level.FINE, "   ├─ InjectionManager ← 5 dependências");

        // ============================================================
        // instanceCreator
        // ============================================================
        instanceCreator.setReflectionCache(reflectionCache);
        instanceCreator.setReflectionProcessor(reflectionProcessor);
        instanceCreator.setDependencyResolver(dependencyResolver);
        instanceCreator.setInjectionManager(injectionManager);
        instanceCreator.setLifecycleManager(lifecycleManager);
        instanceCreator.setScopeManager(scopeManager);
        instanceCreator.setComponentRegistry(beanRegistry);
        instanceCreator.setEventPublisher(eventPublisher);
        instanceCreator.setStrategyManager(strategyManager);
        LOGGER.log(Level.FINE, "   ├─ InstanceCreator ← 9 dependências");

        // ============================================================
        // strategyManager
        // ============================================================
        strategyManager.setDependencyResolver(dependencyResolver);
        LOGGER.log(Level.FINE, "   ├─ InstantiationStrategyManager ← 1 dependência");

        // ============================================================
        // 🆕 INICIALIZA INJECTORS (APÓS TODOS OS SETTERS!)
        // ============================================================
        injectionManager.initCoreInjectors();
        LOGGER.log(Level.FINE, "   ├─ InjectionManager.initCoreInjectors() ✅");

        LOGGER.log(Level.INFO, "   ✅ Injeção concluída. Todos os componentes conectados.");
    }

    /**
     * Valida que nenhum componente crítico ficou null.
     */
    private void validate() {
        LOGGER.log(Level.INFO, "🛡️ BootSequence — Validando integridade...");
        StringBuilder erros = new StringBuilder();

        checkNotNull(dependencyResolver, "dependencyResolver", erros);
        checkNotNull(injectionManager, "injectionManager", erros);
        checkNotNull(instanceCreator, "instanceCreator", erros);
        checkNotNull(strategyManager, "strategyManager", erros);
        checkNotNull(beanRegistry, "beanRegistry", erros);  // 🔄 Nome corrigido
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

        LOGGER.log(Level.INFO, "   ✅ Todos os 11 componentes validados.");
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
        LOGGER.log(Level.INFO, "🔍 BootSequence — Inicializando ciclo de vida...");
        lifecycleManager.initialize();
        LOGGER.log(Level.INFO, "   ✅ LifecycleManager inicializado. {0} beans no registry.",
                beanRegistry.getBeanNames().size());
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
                // 🗑️ componentScanner REMOVIDO
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
            // 🗑️ ComponentScanner componentScanner,  // REMOVIDO
            BeanMetadataExtractor metadataExtractor
    ) {
        public int getBeanCount() {
            return beanRegistry.getBeanNames().size();
        }
    }
}