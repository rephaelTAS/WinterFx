package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.anotations.PostConstruct;
import com.ossobo.winterfx.di.configuration.ConfigurationManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Orquestrador central do processo de injeção de dependências.
 *
 * <p>Versão 6.2 — Contratos verificados em initCoreInjectors(): o erro mais
 * traiçoeiro desta arquitetura era chamar initCoreInjectors() antes de
 * setConfigurationManager(), deixando TODO @Value silenciosamente inativo.
 * Agora isso é erro de boot imediato.</p>
 *
 * <p>ORDEM DE INJEÇÃO por bean (contrato do inject()):</p>
 * <ol>
 *   <li>ValueInjector (@Value — precisa vir antes, pois @PostConstruct usa os valores)</li>
 *   <li>FieldInjector (@Inject)</li>
 *   <li>MethodInjector</li>
 *   <li>ExternalInjectors (view, imagem, etc.)</li>
 *   <li>@PostConstruct (por último, com tudo pronto)</li>
 * </ol>
 */
public final class InjectionManager {

    private ReflectionCache reflectionCache;
    private ReflectionProcessor reflectionProcessor;
    private DependencyResolver dependencyResolver;
    private ConfigurationManager configurationManager;
    private LifecycleEventPublisher eventPublisher;

    private ValueInjector valueInjector;
    private FieldInjector fieldInjector;
    private MethodInjector methodInjector;

    private final List<DependencyInjector> externalInjectors = new CopyOnWriteArrayList<>();

    // WeakHashMap: Controllers (Prototypes) podem ser garbage coletados quando a View fecha
    private final Map<Object, Boolean> initialized = Collections.synchronizedMap(new WeakHashMap<>());

    public InjectionManager() {}

    public InjectionManager(ReflectionCache reflectionCache,
                            ReflectionProcessor reflectionProcessor,
                            ConfigurationManager configurationManager,
                            LifecycleEventPublisher eventPublisher) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.configurationManager = configurationManager;
        this.eventPublisher = eventPublisher;
    }

    public void setReflectionCache(ReflectionCache reflectionCache) { this.reflectionCache = reflectionCache; }
    public void setReflectionProcessor(ReflectionProcessor reflectionProcessor) { this.reflectionProcessor = reflectionProcessor; }
    public void setDependencyResolver(DependencyResolver dependencyResolver) { this.dependencyResolver = dependencyResolver; }
    public void setConfigurationManager(ConfigurationManager configurationManager) { this.configurationManager = configurationManager; }
    public void setEventPublisher(LifecycleEventPublisher eventPublisher) { this.eventPublisher = eventPublisher; }

    /**
     * Cria os injetores core. PRÉ-CONDIÇÕES verifiables: todos os setters
     * devem ter sido chamados antes. Violar a ordem agora gera erro imediato
     * e autoexplicativo, em vez de @Value inativo silenciosamente.
     */
    public void initCoreInjectors() {
        Objects.requireNonNull(configurationManager,
                "initCoreInjectors() chamado ANTES de setConfigurationManager() — " +
                        "os @Value não funcionariam. Corrija a ordem no BootSequence/DiContainer.");
        Objects.requireNonNull(reflectionCache, "reflectionCache é obrigatório antes de initCoreInjectors()");
        Objects.requireNonNull(reflectionProcessor, "reflectionProcessor é obrigatório antes de initCoreInjectors()");
        Objects.requireNonNull(dependencyResolver, "dependencyResolver é obrigatório antes de initCoreInjectors()");

        this.valueInjector = new ValueInjector(reflectionCache, reflectionProcessor, configurationManager);
        this.fieldInjector = new FieldInjector(reflectionCache, reflectionProcessor, dependencyResolver);
        this.methodInjector = new MethodInjector(reflectionCache, reflectionProcessor, dependencyResolver);
    }

    public void registerExternalInjector(DependencyInjector injector) {
        externalInjectors.add(Objects.requireNonNull(injector, "injector não pode ser nulo"));
    }

    public boolean unregisterExternalInjector(DependencyInjector injector) { return externalInjectors.remove(injector); }
    public int getExternalInjectorCount() { return externalInjectors.size(); }

    public void inject(Object instance) {
        if (instance == null || initialized.containsKey(instance)) {
            return;
        }

        var type = instance.getClass();

        if (valueInjector != null) valueInjector.inject(instance, type);
        if (fieldInjector != null) fieldInjector.inject(instance, type);
        if (methodInjector != null) methodInjector.inject(instance, type);

        for (var externalInjector : externalInjectors) {
            externalInjector.inject(instance, type);
        }

        processPostConstruct(instance);
        initialized.put(instance, Boolean.TRUE);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(type, null, DependencyLifecycleListener.LifecycleEventType.AFTER_INJECTION, instance);
        }
    }

    private void processPostConstruct(Object instance) {
        var clazz = instance.getClass();
        for (var method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                try {
                    if (!method.canAccess(instance)) method.setAccessible(true);
                    if (method.getParameterCount() == 0) {
                        method.invoke(instance);
                    } else {
                        throw new IllegalStateException("@PostConstruct não pode ter parâmetros: " + clazz.getName() + "." + method.getName());
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("Falha crítica ao executar @PostConstruct: " + clazz.getName() + "." + method.getName(), e);
                }
            }
        }
    }

    public void processPostConstructWithInitialize(Object instance) {
        if (instance == null || initialized.containsKey(instance)) return;
        processPostConstruct(instance);
        initialized.put(instance, Boolean.TRUE);
    }

    public boolean isInitialized(Object instance) { return initialized.containsKey(instance); }
    public void clearInitializedCache() { initialized.clear(); }
    public void clearExternalInjectors() { externalInjectors.clear(); }
}