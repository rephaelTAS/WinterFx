package com.ossobo.winterfx.di.instantiation;

import com.ossobo.winterfx.di.aot.InstanceFactory;
import com.ossobo.winterfx.di.exceptions.DependencyResolutionException;
import com.ossobo.winterfx.di.injection.InjectionManager;
import com.ossobo.winterfx.di.lifecycle.LifecycleManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.resolver.DependencyResolver;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.di.scopes.implementations.SingletonScope;
import com.ossobo.winterfx.scanner.enums.ScopeType;
import com.ossobo.winterfx.scanner.models.BeanDefinition;
import com.ossobo.winterfx.scanner.models.InjectionPoint;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Responsável pela criação e preparação de instâncias de beans gerenciados pelo contêiner.
 *
 * <p>Esta classe orquestra o fluxo principal de criação de um bean: resolução da definição,
 * escolha da estratégia de instanciação, injeção de dependências e invocação de métodos
 * de ciclo de vida ({@code @PostConstruct}).</p>
 *
 * <p><b>Desacoplamento de Proxies:</b> A aplicação de interceptadores (proxies AOP)
 * <b>não</b> é realizada nesta classe. Essa responsabilidade é delegada ao
 * {@code BeanPostProcessor} registrado no {@code DiContainer}, garantindo que o
 * mecanismo de criação de instâncias permaneça isolado do módulo de interceptação.</p>
 *
 * <p><b>Registro Dinâmico (On-the-fly):</b> Caso uma classe solicitada não tenha sido
 * descoberta pelo scanner de componentes, esta classe é capaz de gerar uma
 * {@link BeanDefinition} mínima em tempo de execução, registrá-la no {@link BeanRegistry}
 * e prosseguir com a criação da instância.</p>
 *
 * @version 3.0
 */
public final class InstanceCreator {

    private InjectionManager injectionManager;
    private LifecycleManager lifecycleManager;
    private ScopeManager scopeManager;
    private BeanRegistry beanRegistry;
    private LifecycleEventPublisher eventPublisher;
    private InstantiationStrategyManager strategyManager;
    private DependencyResolver dependencyResolver;

    // ==================== CONSTRUTORES ====================

    /**
     * Construtor padrão.
     */
    public InstanceCreator() {}

    /**
     * Construtor com as dependências principais para a criação de instâncias.
     *
     * @param injectionManager Gerenciador responsável pela injeção de dependências.
     * @param lifecycleManager Gerenciador de ciclo de vida dos beans.
     * @param scopeManager      Gerenciador de escopos (Singleton, Protótipo, etc).
     * @param beanRegistry      Registro de definições de beans do contêiner.
     * @param eventPublisher    Publicador de eventos do ciclo de vida.
     * @param strategyManager   Gerenciador de estratégias de instanciação.
     */
    public InstanceCreator(InjectionManager injectionManager,
                           LifecycleManager lifecycleManager,
                           ScopeManager scopeManager,
                           BeanRegistry beanRegistry,
                           LifecycleEventPublisher eventPublisher,
                           InstantiationStrategyManager strategyManager) {
        this.injectionManager = injectionManager;
        this.lifecycleManager = lifecycleManager;
        this.scopeManager = scopeManager;
        this.beanRegistry = beanRegistry;
        this.eventPublisher = eventPublisher;
        this.strategyManager = strategyManager;
    }

    // ==================== SETTERS ====================

    public void setDependencyResolver(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    public void setInjectionManager(InjectionManager injectionManager) {
        this.injectionManager = injectionManager;
    }

    public void setLifecycleManager(LifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }

    public void setScopeManager(ScopeManager scopeManager) {
        this.scopeManager = scopeManager;
    }

    public void setComponentRegistry(BeanRegistry beanRegistry) {
        this.beanRegistry = beanRegistry;
    }

    public void setEventPublisher(LifecycleEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void setStrategyManager(InstantiationStrategyManager strategyManager) {
        this.strategyManager = strategyManager;
    }

    // ==================== CRIAÇÃO PRINCIPAL ====================

    /**
     * Cria uma instância completa de um bean, executando o fluxo de estratégia de
     * instanciação, injeção de dependências e ciclo de vida.
     *
     * <p>O método tenta utilizar uma fábrica AOT (Ahead-Of-Time) se disponível.
     * Caso contrário, verifica se a definição existe no registro (criando uma dinamicamente
     * se necessário) e utiliza a estratégia padrão do {@link InstantiationStrategyManager}.</p>
     *
     * <p><b>Nota:</b> O proxy não é aplicado nesta etapa.</p>
     *
     * @param <T>  O tipo do bean a ser criado.
     * @param type A classe do bean.
     * @return A instância do bean totalmente inicializada e com dependências injetadas.
     * @throws DependencyResolutionException Se ocorrer qualquer falha durante a resolução
     *                                       ou instanciação do bean.
     */
    @SuppressWarnings("unchecked")
    public <T> T createInstance(Class<T> type) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(type, null,
                    DependencyLifecycleListener.LifecycleEventType.BEFORE_CREATION, type);
        }

        try {
            BeanDefinition definition = beanRegistry.getDefinition(type);

            if (definition != null) {
                InstanceFactory<T> aotFactory = (InstanceFactory<T>) beanRegistry.getAotFactory(type);
                if (aotFactory != null) {
                    T instance = aotFactory.create(dependencyResolver);
                    injectionManager.inject(instance);
                    lifecycleManager.invokePostConstruct(instance);
                    publishAfterCreation(type, instance);
                    return instance;
                }
            }

            if (definition == null) {
                definition = registerOnTheFly(type);
            }

            T instance = (T) createWithStrategy(definition);
            registerEarlyReference(type, instance);
            injectionManager.inject(instance);
            lifecycleManager.invokePostConstruct(instance);

            publishAfterCreation(type, instance);
            return instance;

        } catch (DependencyResolutionException e) {
            throw e;
        } catch (Exception e) {
            if (eventPublisher != null) {
                eventPublisher.publishEvent(type, null,
                        DependencyLifecycleListener.LifecycleEventType.LIFECYCLE_ERROR, null, e);
            }
            throw new DependencyResolutionException(
                    "Falha ao criar instância de " + type.getName(), e);
        }
    }

    /**
     * Aplica injeção de dependências e invoca o ciclo de {@code @PostConstruct}
     * em uma instância previamente criada fora do fluxo padrão do contêiner.
     *
     * @param instance A instância a ser processada.
     * @return A mesma instância, após a injeção e inicialização.
     */
    public Object injectAndPostConstruct(Object instance) {
        if (instance == null) return null;
        injectionManager.inject(instance);
        lifecycleManager.invokePostConstruct(instance);
        return instance;
    }

    // ==================== ESTRATÉGIA ====================

    /**
     * Delega a criação da instância bruta para a estratégia adequada fornecida
     * pelo {@link InstantiationStrategyManager}.
     *
     * @param definition A definição do bean.
     * @return A instância bruta do bean, sem injeção de dependências.
     * @throws DependencyResolutionException Se não houver estratégia compatível ou se a instanciação falhar.
     */
    private Object createWithStrategy(BeanDefinition definition) {
        InstantiationStrategy strategy = strategyManager.getStrategy(definition);
        if (strategy == null) {
            throw new DependencyResolutionException(
                    "Nenhuma estratégia para: " + definition.getName());
        }
        try {
            return strategy.instantiate(definition);
        } catch (Exception e) {
            throw new DependencyResolutionException(
                    "Falha ao instanciar " + definition.getName(), e);
        }
    }

    // ==================== REGISTRO ANTECIPADO ====================

    /**
     * Registra a instância no escopo singleton precocemente para permitir a
     * resolução de dependências circulares.
     *
     * @param <T>      O tipo do bean.
     * @param type     A classe do bean.
     * @param instance A instância a ser registrada antecipadamente.
     */
    @SuppressWarnings("unchecked")
    private <T> void registerEarlyReference(Class<?> type, T instance) {
        SingletonScope singletonScope = scopeManager.getSingletonScope();
        if (singletonScope != null) {
            singletonScope.putEarly((Class<T>) type, instance);
        }
    }

    // ==================== REGISTRO ON-THE-FLY ====================

    /**
     * Gera e registra uma {@link BeanDefinition} mínima para classes que não foram
     * detectadas pelo scanner de componentes.
     *
     * <p>Utiliza reflexão básica para identificar métodos anotados com
     * {@code @PostConstruct} e {@code @PreDestroy}, assumindo escopo padrão
     * {@link ScopeType#SINGLETON} e ausência de pontos de injeção explícitos
     * no nível da definição (eles serão resolvidos em tempo de injeção).</p>
     *
     * @param type A classe a ser registrada dinamicamente.
     * @return A {@link BeanDefinition} gerada e registrada no {@link BeanRegistry}.
     */
    private BeanDefinition registerOnTheFly(Class<?> type) {
        String name = Character.toLowerCase(type.getSimpleName().charAt(0))
                + type.getSimpleName().substring(1);

        List<InjectionPoint> deps = Collections.emptyList();
        Method postConstruct = null;
        Method preDestroy = null;

        try {
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(com.ossobo.winterfx.anotations.PostConstruct.class)
                        && postConstruct == null) {
                    method.setAccessible(true);
                    postConstruct = method;
                }
                if (method.isAnnotationPresent(com.ossobo.winterfx.anotations.PreDestroy.class)
                        && preDestroy == null) {
                    method.setAccessible(true);
                    preDestroy = method;
                }
            }
        } catch (Exception ignored) {
        }

        BeanDefinition definition = new BeanDefinition(
                name, type, ScopeType.SINGLETON,
                deps, postConstruct, preDestroy,
                false, null, Collections.emptyMap()
        );
        beanRegistry.registerDefinition(definition);
        return definition;
    }

    // ==================== EVENTO ====================

    /**
     * Publica o evento de conclusão do ciclo de pós-construção.
     *
     * @param type     O tipo do bean.
     * @param instance A instância criada.
     */
    private void publishAfterCreation(Class<?> type, Object instance) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(type, null,
                    DependencyLifecycleListener.LifecycleEventType.AFTER_POST_CONSTRUCT, instance);
        }
    }
}