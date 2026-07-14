package com.ossobo.winterfx.di;

import com.ossobo.winterfx.di.configuration.ConfigurationManager;
import com.ossobo.winterfx.di.injection.DependencyInjector;
import com.ossobo.winterfx.di.injection.InjectionManager;
import com.ossobo.winterfx.di.instantiation.InstanceCreator;
import com.ossobo.winterfx.di.instantiation.InstantiationStrategyManager;
import com.ossobo.winterfx.di.lifecycle.LifecycleManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.di.resolver.methods.CircularDependencyDetector;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.di.scopes.implementations.SingletonScope;
import com.ossobo.winterfx.runtime.BeanPostProcessor;
import com.ossobo.winterfx.scanner.enums.ScopeType;
import com.ossobo.winterfx.scanner.models.BeanDefinition;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fachada principal e ponto de entrada único para o módulo de Injeção de Dependências.
 *
 * <p>Implementa o padrão Singleton e atua como a porta de comunicação entre o subsistema
 * de DI e os demais módulos do framework (como views, gerenciadores de imagens e recursos).
 * É responsável por orquestrar a inicialização via {@link BootSequence}, fornecer a API
 * de resolução de beans e gerenciar o cache primário de instâncias singleton.</p>
 *
 * <p>O contêiner é completamente desacoplado de módulos de alto nível, permitindo que
 * outras partes do framework se estendam registrando {@link DependencyInjector} externos
 * e {@link BeanPostProcessor} para processamento AOP.</p>
 *
 * @version 6.0
 */
public final class DiContainer {

    private static volatile DiContainer INSTANCE;

    // ============================================================
    // CACHE DE INSTÂNCIAS
    // ============================================================
    private final Map<Class<?>, Object> singletonCacheByType = new ConcurrentHashMap<>();
    private final Map<String, Object> singletonCacheByName = new ConcurrentHashMap<>();
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    // ============================================================
    // COMPONENTES DO DI
    // ============================================================
    private ScopeManager scopeManager;
    private ReflectionCache reflectionCache;
    private ReflectionProcessor reflectionProcessor;
    private LifecycleEventPublisher eventPublisher;
    private CircularDependencyDetector circularDetector;
    private ConfigurationManager configurationManager;
    private BeanRegistry beanRegistry;
    private ResourceRegistry resourceRegistry;
    private LifecycleManager lifecycleManager;
    private InjectionManager injectionManager;
    private InstanceCreator instanceCreator;
    private InstantiationStrategyManager strategyManager;
    private DependencyResolver dependencyResolver;

    // ============================================================
    // CONSTRUTOR
    // ============================================================

    private DiContainer(BeanRegistry beanRegistry, ResourceRegistry resourceRegistry) {
        this.beanRegistry = beanRegistry;
        this.resourceRegistry = resourceRegistry;
    }

    // ============================================================
    // INICIALIZAÇÃO
    // ============================================================

    /**
     * Inicializa o singleton do contêiner de injeção de dependências.
     *
     * <p>Utiliza Double-Checked Locking para garantir a criação segura em ambientes
     * multi-threaded. Executa o processo de bootstrap completo através da {@link BootSequence}.</p>
     *
     * @param beanRegistry    O registro de beans descobertos pelo scanner.
     * @param resourceRegistry O registro de recursos (views, imagens, estilos) descobertos pelo scanner.
     */
    public static void initialize(BeanRegistry beanRegistry, ResourceRegistry resourceRegistry) {
        if (INSTANCE == null) {
            synchronized (DiContainer.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DiContainer(beanRegistry, resourceRegistry);
                    INSTANCE.boot();
                }
            }
        }
    }

    /**
     * Executa a sequência de inicialização e conecta os componentes internos
     * retornados pela {@link BootSequence}.
     */
    private void boot() {
        BootSequence sequence = new BootSequence(beanRegistry);
        BootSequence.BootResult result = sequence.boot();

        this.dependencyResolver = result.dependencyResolver();
        this.injectionManager = result.injectionManager();
        this.instanceCreator = result.instanceCreator();
        this.strategyManager = result.strategyManager();
        this.beanRegistry = result.beanRegistry();
        this.scopeManager = result.scopeManager();
        this.lifecycleManager = result.lifecycleManager();
        this.configurationManager = result.configurationManager();
        this.reflectionCache = result.reflectionCache();
        this.reflectionProcessor = result.reflectionProcessor();
        this.eventPublisher = result.eventPublisher();
        this.circularDetector = result.circularDetector();
    }

    // ============================================================
    // API — RESOLUÇÃO DE BEANS
    // ============================================================

    /**
     * Recupera um bean gerenciado pelo seu tipo de classe.
     *
     * <p>Se o bean já estiver no cache primário do contêiner, ele é retornado imediatamente.
     * Caso contrário, a resolução é delegada ao {@link DependencyResolver} e o resultado
     * é armazenado em cache.</p>
     *
     * @param <T>  O tipo do bean.
     * @param type A classe do bean desejado.
     * @return A instância gerenciada do bean.
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        if (singletonCacheByType.containsKey(type)) {
            return (T) singletonCacheByType.get(type);
        }
        T instance = dependencyResolver.getBean(type);
        singletonCacheByType.put(type, instance);
        String beanName = Character.toLowerCase(type.getSimpleName().charAt(0))
                + type.getSimpleName().substring(1);
        singletonCacheByName.put(beanName, instance);
        return instance;
    }

    /**
     * Recupera um bean gerenciado exclusivamente pelo seu nome lógico.
     *
     * @param <T>  O tipo do bean.
     * @param name O nome lógico do bean.
     * @return A instância gerenciada do bean.
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        if (singletonCacheByName.containsKey(name)) {
            return (T) singletonCacheByName.get(name);
        }
        T instance = dependencyResolver.getBean(name);
        if (instance != null) {
            singletonCacheByName.put(name, instance);
            singletonCacheByType.put(instance.getClass(), instance);
        }
        return instance;
    }

    /**
     * Recupera um bean gerenciado combinando seu tipo e um qualificador específico.
     *
     * @param <T>       O tipo do bean.
     * @param type      A classe do bean.
     * @param qualifier O nome do qualificador.
     * @return A instância gerenciada do bean.
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type, String qualifier) {
        String key = type.getName() + "#" + qualifier;
        if (singletonCacheByName.containsKey(key)) {
            return (T) singletonCacheByName.get(key);
        }
        T instance = dependencyResolver.getBean(type, qualifier);
        if (instance != null) {
            singletonCacheByName.put(key, instance);
            singletonCacheByType.put(type, instance);
        }
        return instance;
    }

    /**
     * Recupera todas as instâncias gerenciadas que implementam ou estendem um tipo específico.
     *
     * @param <T>  O tipo base.
     * @param type A classe ou interface base.
     * @return Uma lista com todas as instâncias compatíveis encontradas.
     */
    public <T> List<T> getAllBeansOfType(Class<T> type) {
        return dependencyResolver.getAllBeansOfType(type);
    }

    // ============================================================
    // API — REGISTRO
    // ============================================================

    /**
     * Registra manualmente uma instância existente no contêiner como um bean Singleton.
     *
     * <p>A instância é armazenada nos caches internos do contêiner, no escopo singleton
     * do {@link ScopeManager}, e uma definição de bean é registrada no {@link BeanRegistry}.</p>
     *
     * @param <T>      O tipo do bean.
     * @param type     A classe do bean.
     * @param instance A instância a ser gerenciada.
     */
    public <T> void register(Class<T> type, T instance) {
        singletonCacheByType.put(type, instance);
        String name = Character.toLowerCase(type.getSimpleName().charAt(0))
                + type.getSimpleName().substring(1);
        singletonCacheByName.put(name, instance);

        SingletonScope singletonScope = scopeManager.getSingletonScope();
        if (singletonScope != null) {
            singletonScope.put(type, instance);
        }

        BeanDefinition definition = new BeanDefinition(
                name, type, ScopeType.SINGLETON,
                Collections.emptyList(), null, null,
                false, null, Collections.emptyMap());
        beanRegistry.registerDefinition(definition);
        lifecycleManager.notifyBeanRegistered(type, name);
    }

    // ============================================================
    // API — INJEÇÃO
    // ============================================================

    /**
     * Executa a injeção de dependências em uma instância que não foi criada pelo contêiner.
     *
     * <p>Útil para objetos criados pelo JavaFX (como Controllers FXML) ou instâncias
     * externas que necessitam ter suas anotações {@code @Inject} e {@code @Value} processadas.</p>
     *
     * @param target A instância a receber a injeção.
     * @throws IllegalArgumentException Se o alvo fornecido for nulo.
     */
    public void injectDependencies(Object target) {
        if (target == null) {
            throw new IllegalArgumentException("Target não pode ser nulo");
        }
        injectionManager.inject(target);
    }

    // ============================================================
    // API — INJECTORS EXTERNOS
    // ============================================================

    /**
     * Registra um injetor de dependências especializado pertencente a um módulo externo do framework.
     *
     * <p>Permite que módulos como gerenciamento de views ou imagens estendam a capacidade
     * de injeção para anotações específicas (ex: {@code @InjectView}, {@code @InjectImage}).</p>
     *
     * @param injector O injetor externo a ser registrado.
     */
    public void registerExternalInjector(DependencyInjector injector) {
        injectionManager.registerExternalInjector(injector);
    }

    // ============================================================
    // API — BEAN POST PROCESSORS
    // ============================================================

    /**
     * Registra um processador pós-criação de beans (BeanPostProcessor).
     *
     * <p>Estes processadores são tipicamente utilizados pelo módulo de interceptação
     * para aplicar proxies AOP (Aspect-Oriented Programming) nas instâncias criadas.</p>
     *
     * @param processor O processador a ser registrado.
     */
    public void registerBeanPostProcessor(BeanPostProcessor processor) {
        if (processor != null) {
            beanPostProcessors.add(processor);
        }
    }

    /**
     * Retorna a lista imutável de processadores de beans registrados.
     *
     * @return Uma lista não modificável de {@link BeanPostProcessor}.
     */
    public List<BeanPostProcessor> getBeanPostProcessors() {
        return Collections.unmodifiableList(beanPostProcessors);
    }

    // ============================================================
    // API — CICLO DE VIDA
    // ============================================================

    /**
     * Adiciona um ouvinte para monitorar os eventos do ciclo de vida dos beans do contêiner.
     *
     * @param listener O ouvinte a ser registrado.
     */
    public void addLifecycleListener(DependencyLifecycleListener listener) {
        lifecycleManager.addListener(listener);
    }

    /**
     * Atualiza o estado do contêiner, republicando o evento de inicialização.
     */
    public void refresh() {
        lifecycleManager.initialize();
    }

    /**
     * Executa o desligamento ordenado do contêiner de injeção.
     *
     * <p>Invoca a destruição de todos os escopos e beans gerenciados, limpa os caches
     * internos, os registros e reseta a instância singleton.</p>
     */
    public void close() {
        lifecycleManager.shutdown();
        if (reflectionCache != null) reflectionCache.clear();
        if (beanRegistry != null) beanRegistry.clear();
        singletonCacheByType.clear();
        singletonCacheByName.clear();
        INSTANCE = null;
    }

    // ============================================================
    // GETTERS
    // ============================================================

    /**
     * Retorna o gerenciador de configurações do contêiner.
     *
     * @return O {@link ConfigurationManager}.
     */
    public ConfigurationManager getConfiguration() { return configurationManager; }

    /**
     * Retorna o registro de beans do contêiner.
     *
     * @return O {@link BeanRegistry}.
     */
    public BeanRegistry getBeanRegistry() { return beanRegistry; }

    /**
     * Retorna o registro de recursos do framework.
     *
     * @return O {@link ResourceRegistry}.
     */
    public ResourceRegistry getResourceRegistry() { return resourceRegistry; }

    /**
     * Retorna o gerenciador de escopos do DI.
     *
     * @return O {@link ScopeManager}.
     */
    public ScopeManager getScopeManager() { return scopeManager; }

    /**
     * Retorna o gerenciador de ciclo de vida dos beans.
     *
     * @return O {@link LifecycleManager}.
     */
    public LifecycleManager getLifecycleManager() { return lifecycleManager; }

    /**
     * Retorna o resolvedor central de dependências.
     *
     * @return O {@link DependencyResolver}.
     */
    public DependencyResolver getDependencyResolver() { return dependencyResolver; }

    /**
     * Retorna o gerenciador de injeção de dependências.
     *
     * @return O {@link InjectionManager}.
     */
    public InjectionManager getInjectionManager() { return injectionManager; }

    /**
     * Retorna o cache de reflexão utilizado pelo DI.
     *
     * @return O {@link ReflectionCache}.
     */
    public ReflectionCache getReflectionCache() { return reflectionCache; }

    /**
     * Retorna o processador de reflexão utilizado pelo DI.
     *
     * @return O {@link ReflectionProcessor}.
     */
    public ReflectionProcessor getReflectionProcessor() { return reflectionProcessor; }

    /**
     * Verifica se uma instância de um determinado tipo já está armazenada no cache primário do contêiner.
     *
     * @param type A classe a ser verificada.
     * @return {@code true} se a instância estiver em cache, {@code false} caso contrário.
     */
    public boolean isBeanCached(Class<?> type) {
        return singletonCacheByType.containsKey(type);
    }
    /**
     * Retorna a instância singleton do contêiner de injeção de dependências.
     *
     * @return A instância única do {@link DiContainer}.
     * @throws IllegalStateException Se o contêiner ainda não tiver sido inicializado via {@link #initialize}.
     */
    public static DiContainer getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "DiContainer não inicializado. Chame DiContainer.initialize() primeiro.");
        }
        return INSTANCE;
    }
}