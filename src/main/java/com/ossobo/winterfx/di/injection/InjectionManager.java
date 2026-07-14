package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.anotations.PostConstruct;
import com.ossobo.winterfx.di.configuration.ConfigurationManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Orquestrador central do processo de injeção de dependências do contêiner.
 *
 * <p>Esta classe coordena a execução de todos os injetores registrados, garantindo
 * que a injeção ocorra em uma ordem estrita e previsível para evitar problemas
 * de dependência circular ou inicialização prematura:</p>
 * <ol>
 *   <li>Injeção de propriedades de configuração ({@code @Value})</li>
 *   <li>Injeção de dependências em campos ({@code @Inject})</li>
 *   <li>Injeção de dependências em métodos ({@code @Inject})</li>
 *   <li>Execução de injetores externos registrados por outros módulos do framework</li>
 *   <li>Invocação de métodos de ciclo de vida ({@code @PostConstruct})</li>
 * </ol>
 *
 * <p>Projetado sob o princípio de desacoplamento, não possui conhecimento direto
 * de módulos de alto nível (como gerenciadores de view ou imagens). Módulos externos
 * devem se registrar utilizando o método {@link #registerExternalInjector(DependencyInjector)}.</p>
 *
 * <p>A classe é thread-safe e implementa proteção contra dupla inicialização
 * para garantir que um mesmo bean não passe pelo processo de injeção mais de uma vez.</p>
 *
 * @version 4.0
 */
public final class InjectionManager {

    // ==================== DEPENDÊNCIAS DO PRÓPRIO DI ====================

    private ReflectionCache reflectionCache;
    private ReflectionProcessor reflectionProcessor;
    private DependencyResolver dependencyResolver;
    private ConfigurationManager configurationManager;
    private LifecycleEventPublisher eventPublisher;

    // ==================== INJECTORS CORE (DO DI) ====================

    private ValueInjector valueInjector;
    private FieldInjector fieldInjector;
    private MethodInjector methodInjector;

    // ==================== INJECTORS EXTERNOS ====================

    private final List<DependencyInjector> externalInjectors = new CopyOnWriteArrayList<>();

    // ==================== PROTEÇÃO CONTRA DUPLA INICIALIZAÇÃO ====================

    private final Map<Object, Boolean> initialized = new ConcurrentHashMap<>();

    // ==================== CONSTRUTORES ====================

    /**
     * Construtor padrão.
     */
    public InjectionManager() {
    }

    /**
     * Construtor com as dependências principais do subsistema de injeção.
     *
     * @param reflectionCache       Cache de metadados de reflexão.
     * @param reflectionProcessor    Processador de operações reflexivas.
     * @param configurationManager  Gerenciador de configurações para resolução de {@code @Value}.
     * @param eventPublisher        Publicador de eventos de ciclo de vida do bean.
     */
    public InjectionManager(ReflectionCache reflectionCache,
                            ReflectionProcessor reflectionProcessor,
                            ConfigurationManager configurationManager,
                            LifecycleEventPublisher eventPublisher) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.configurationManager = configurationManager;
        this.eventPublisher = eventPublisher;
    }

    // ==================== SETTERS (SÓ DO DI) ====================

    /**
     * Define o cache de reflexão utilizado pelos injetores internos.
     *
     * @param reflectionCache O cache de reflexão.
     */
    public void setReflectionCache(ReflectionCache reflectionCache) {
        this.reflectionCache = reflectionCache;
    }

    /**
     * Define o processador de reflexão utilizado pelos injetores internos.
     *
     * @param reflectionProcessor O processador de reflexão.
     */
    public void setReflectionProcessor(ReflectionProcessor reflectionProcessor) {
        this.reflectionProcessor = reflectionProcessor;
    }

    /**
     * Define o resolvedor de dependências utilizado pelos injetores de campos e métodos.
     *
     * @param dependencyResolver O resolvedor de dependências do contêiner.
     */
    public void setDependencyResolver(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    /**
     * Define o gerenciador de configurações utilizado pelo injetor de valores.
     *
     * @param configurationManager O gerenciador de configurações.
     */
    public void setConfigurationManager(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    /**
     * Define o publicador de eventos de ciclo de vida.
     *
     * @param eventPublisher O publicador de eventos.
     */
    public void setEventPublisher(LifecycleEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    // ==================== INICIALIZAÇÃO DE INJECTORS CORE ====================

    /**
     * Instancia e configura os injetores internos padrão do contêiner de DI.
     *
     * <p>Este método deve ser invocado pelo {@code DiContainer} durante a fase
     * de bootstrap, após a definição de todas as dependências via setters.</p>
     */
    public void initCoreInjectors() {
        this.valueInjector = new ValueInjector(reflectionCache, reflectionProcessor, configurationManager);
        this.fieldInjector = new FieldInjector(reflectionCache, reflectionProcessor, dependencyResolver);
        this.methodInjector = new MethodInjector(reflectionCache, reflectionProcessor, dependencyResolver);
    }

    // ==================== REGISTRO DE INJECTORS EXTERNOS ====================

    /**
     * Registra um injetor de dependências externo ao núcleo do DI.
     *
     * <p>Permite que outros módulos do framework estendam a capacidade de injeção
     * sem acoplamento direto. Os injetores externos são executados após os injetores
     * padrão de campos e métodos, mas antes do {@code @PostConstruct}.</p>
     *
     * @param injector O injetor especializado a ser registrado.
     * @throws NullPointerException se o injetor fornecido for nulo.
     */
    public void registerExternalInjector(DependencyInjector injector) {
        if (injector == null) {
            throw new NullPointerException("injector não pode ser nulo");
        }
        externalInjectors.add(injector);
    }

    /**
     * Remove um injetor externo previamente registrado.
     *
     * @param injector O injetor a ser removido.
     * @return {@code true} se o injetor foi encontrado e removido, {@code false} caso contrário.
     */
    public boolean unregisterExternalInjector(DependencyInjector injector) {
        return externalInjectors.remove(injector);
    }

    /**
     * Retorna a quantidade de injetores externos atualmente registrados.
     *
     * @return O número de injetores externos.
     */
    public int getExternalInjectorCount() {
        return externalInjectors.size();
    }

    // ==================== INJEÇÃO PRINCIPAL ====================

    /**
     * Executa o ciclo completo de injeção de dependências em uma instância de bean.
     *
     * <p>A injeção segue a ordem estrita: {@code @Value}, campos {@code @Inject},
     * métodos {@code @Inject}, injetores externos e, por fim, {@code @PostConstruct}.</p>
     *
     * <p>Implementa proteção contra dupla inicialização: se a instância já tiver
     * sido processada por este gerenciador, a execução retorna imediatamente.</p>
     *
     * @param instance A instância do bean a ser processada.
     */
    public void inject(Object instance) {
        if (instance == null) return;

        if (initialized.containsKey(instance)) {
            return;
        }

        Class<?> type = instance.getClass();

        if (valueInjector != null) {
            valueInjector.inject(instance, type);
        }

        if (fieldInjector != null) {
            fieldInjector.inject(instance, type);
        }

        if (methodInjector != null) {
            methodInjector.inject(instance, type);
        }

        for (DependencyInjector externalInjector : externalInjectors) {
            externalInjector.inject(instance, type);
        }

        processPostConstruct(instance);

        initialized.put(instance, Boolean.TRUE);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(type, null,
                    DependencyLifecycleListener.LifecycleEventType.AFTER_INJECTION, instance);
        }
    }

    // ==================== @POSTCONSTRUCT ====================

    /**
     * Invoca todos os métodos anotados com {@code @PostConstruct} na instância fornecida.
     *
     * <p>A invocação é feita via reflexão. Espera-se que os métodos anotados
     * não possuam parâmetros. Falhas na invocação de um método {@code @PostConstruct}
     * são absorvidas silenciosamente para não interromper o processo de inicialização
     * dos demais beans ou métodos.</p>
     *
     * @param instance A instância a ter seus métodos de pós-construção invocados.
     */
    private void processPostConstruct(Object instance) {
        if (instance == null) return;

        Class<?> clazz = instance.getClass();

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                try {
                    method.setAccessible(true);
                    if (method.getParameterCount() == 0) {
                        method.invoke(instance);
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    // ==================== MÉTODO SEGURO PARA FXML SERVICE ====================

    /**
     * Processa os métodos {@code @PostConstruct} respeitando a proteção contra
     * dupla inicialização, mas sem executar os injetores de dependências.
     *
     * <p>Este método é utilizado especificamente pelo {@code FXMLService} para garantir
     * que controllers instanciados diretamente pelo JavaFX tenham seu ciclo de vida
     * de pós-construção executado de forma segura, mesmo não tendo passado pelo
     * fluxo padrão do {@link #inject(Object)}.</p>
     *
     * @param instance A instância do bean a ser processada.
     */
    public void processPostConstructWithInitialize(Object instance) {
        if (instance == null) return;

        if (initialized.containsKey(instance)) {
            return;
        }

        processPostConstruct(instance);

        initialized.put(instance, Boolean.TRUE);
    }

    /**
     * Verifica se uma instância específica já foi processada por este gerenciador.
     *
     * @param instance A instância a ser verificada.
     * @return {@code true} se a instância já foi inicializada, {@code false} caso contrário.
     */
    public boolean isInitialized(Object instance) {
        return initialized.containsKey(instance);
    }

    /**
     * Limpa o registro de instâncias inicializadas.
     *
     * <p>Útil para permitir que um mesmo objeto seja reinjetado ou para liberar
     * memória durante o shutdown da aplicação.</p>
     */
    public void clearInitializedCache() {
        initialized.clear();
    }

    /**
     * Remove todos os injetores externos registrados.
     */
    public void clearExternalInjectors() {
        externalInjectors.clear();
    }
}