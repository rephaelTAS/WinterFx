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
import com.ossobo.winterfx.di.scanner.ComponentRegistry;
import com.ossobo.winterfx.di.scanner.ComponentScanner;
import com.ossobo.winterfx.di.scanner.ReflectionScanner;
import com.ossobo.winterfx.di.scopes.ScopeManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orquestrador de inicialização segura do DiContainer.
 *
 * Usa padrão de 2 fases:
 *   FASE 1 — NASCIMENTO: Constrói todas as classes com construtor vazio.
 *   FASE 2 — INJEÇÃO:   Conecta dependências exclusivamente via setters.
 *
 * NENHUM construtor recebe null. NENHUMA dependência circular.
 * Cada classe nasce vazia e recebe o que precisa depois.
 *
 * @version v2.0 (18/05/2026)
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
    private final ComponentRegistry componentRegistry;
    private final ComponentScanner componentScanner;
    private final LifecycleManager lifecycleManager;

    // ===== COMPONENTES NÍVEL 1 (recebem dependências via setters) =====
    private InjectionManager injectionManager;
    private InstanceCreator instanceCreator;
    private InstantiationStrategyManager strategyManager;
    private DependencyResolver dependencyResolver;

    private final String[] packages;

    /**
     * FASE 1 — NASCIMENTO: Constrói todas as classes.
     *
     * Nível 0: Infraestrutura (zero dependências)
     * Nível 1: Managers (criados vazios, recebem dependências na FASE 2)
     */
    public BootSequence(String... packages) {
        this.packages = packages;
        LOGGER.log(Level.INFO, "🔧 BootSequence FASE 1 — NASCIMENTO: Construindo todas as classes...");

        // ============================================================
        // NÍVEL 0: Infraestrutura — não dependem de ninguém
        // ============================================================
        this.scopeManager = new ScopeManager();
        this.reflectionCache = new ReflectionCache(new ReflectionScanner());
        this.reflectionProcessor = new ReflectionProcessor();
        this.eventPublisher = new LifecycleEventPublisher();
        this.circularDetector = new CircularDependencyDetector();
        this.configurationManager = new ConfigurationManager();
        this.componentRegistry = new ComponentRegistry();
        this.componentScanner = new ComponentScanner(componentRegistry, packages);
        this.lifecycleManager = new LifecycleManager(
                reflectionCache, reflectionProcessor, scopeManager, eventPublisher);

        // ============================================================
        // NÍVEL 1: Managers — construtores vazios, 100% seguros
        // ============================================================
        this.strategyManager = new InstantiationStrategyManager();
        this.injectionManager = new InjectionManager();
        this.instanceCreator = new InstanceCreator();
        this.dependencyResolver = new DependencyResolver();

        LOGGER.log(Level.INFO, "   ✅ 13 componentes construídos (4 vazios).");
    }

    /**
     * FASE 2 — INJEÇÃO: Conecta todas as dependências via setters.
     *
     * Ordem não é crítica — cada setter é independente.
     * Nenhum componente recebe null porque todos já existem.
     */
    private void inject() {
        LOGGER.log(Level.INFO, "🔗 BootSequence FASE 2 — INJEÇÃO: Conectando dependências...");

        // ============================================================
        // dependencyResolver — recebe suas dependências
        // ============================================================
        dependencyResolver.setComponentRegistry(componentRegistry);
        dependencyResolver.setScopeManager(scopeManager);
        dependencyResolver.setInstanceCreator(instanceCreator);
        dependencyResolver.setLifecycleManager(lifecycleManager);
        dependencyResolver.setEventPublisher(eventPublisher);
        dependencyResolver.setCircularDependencyDetector(circularDetector);
        LOGGER.log(Level.FINE, "   ├─ DependencyResolver ← 6 dependências");

        // ============================================================
        // injectionManager — recebe suas dependências
        // ============================================================
        injectionManager.setReflectionCache(reflectionCache);
        injectionManager.setReflectionProcessor(reflectionProcessor);
        injectionManager.setDependencyResolver(dependencyResolver);
        injectionManager.setConfigurationManager(configurationManager);
        injectionManager.setEventPublisher(eventPublisher);
        LOGGER.log(Level.FINE, "   ├─ InjectionManager ← 5 dependências");

        // ============================================================
        // instanceCreator — recebe suas dependências
        // ============================================================
        instanceCreator.setReflectionCache(reflectionCache);
        instanceCreator.setReflectionProcessor(reflectionProcessor);
        instanceCreator.setDependencyResolver(dependencyResolver);
        instanceCreator.setInjectionManager(injectionManager);
        instanceCreator.setLifecycleManager(lifecycleManager);
        instanceCreator.setScopeManager(scopeManager);
        instanceCreator.setComponentRegistry(componentRegistry);
        instanceCreator.setEventPublisher(eventPublisher);
        instanceCreator.setStrategyManager(strategyManager);
        LOGGER.log(Level.FINE, "   ├─ InstanceCreator ← 9 dependências");

        // ============================================================
        // strategyManager — recebe suas dependências
        // ============================================================
        strategyManager.setDependencyResolver(dependencyResolver);
        LOGGER.log(Level.FINE, "   ├─ InstantiationStrategyManager ← 1 dependência");

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
        checkNotNull(componentRegistry, "componentRegistry", erros);
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
     * Executa scan de componentes e registra beans.
     */
    private void scan() {
        LOGGER.log(Level.INFO, "🔍 BootSequence — Escaneando componentes...");
        componentScanner.scanAndRegister();
        lifecycleManager.initialize();
        LOGGER.log(Level.INFO, "   ✅ Scan concluído. {0} beans registrados.",
                componentRegistry.getBeanNames().size());
    }

    /**
     * Executa a sequência completa: NASCIMENTO → INJEÇÃO → VALIDAÇÃO → SCAN.
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
                componentRegistry,
                scopeManager,
                lifecycleManager,
                componentScanner
        );
    }

    /**
     * Resultado do boot com todos os componentes prontos.
     */
    public record BootResult(
            DependencyResolver dependencyResolver,
            InjectionManager injectionManager,
            InstanceCreator instanceCreator,
            InstantiationStrategyManager strategyManager,
            ComponentRegistry componentRegistry,
            ScopeManager scopeManager,
            LifecycleManager lifecycleManager,
            ComponentScanner componentScanner
    ) {
        public int getBeanCount() {
            return componentRegistry.getBeanNames().size();
        }
    }
}