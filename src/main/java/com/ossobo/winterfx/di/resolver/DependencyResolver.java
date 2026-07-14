package com.ossobo.winterfx.di.resolver;

import com.ossobo.winterfx.anotations.Qualifier;
import com.ossobo.winterfx.di.aot.InstanceFactory;
import com.ossobo.winterfx.di.exceptions.BeanNotFoundException;
import com.ossobo.winterfx.di.exceptions.CircularDependencyException;
import com.ossobo.winterfx.di.exceptions.DependencyNotRegisteredException;
import com.ossobo.winterfx.di.instantiation.InstanceCreator;
import com.ossobo.winterfx.di.lifecycle.LifecycleManager;
import com.ossobo.winterfx.di.lifecycle.events.LifecycleEventPublisher;
import com.ossobo.winterfx.di.lifecycle.interfaces.DependencyLifecycleListener;
import com.ossobo.winterfx.di.resolver.methods.CircularDependencyDetector;
import com.ossobo.winterfx.di.scopes.ScopeManager;
import com.ossobo.winterfx.di.scopes.interfaces.ScopeInterface;
import com.ossobo.winterfx.scanner.enums.ScopeType;
import com.ossobo.winterfx.scanner.models.BeanDefinition;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolvedor central de dependências do contêiner de injeção.
 *
 * <p>Esta classe é responsável por localizar, instanciar e gerenciar o ciclo de vida
 * dos beans sob demanda. Ela orquestra a busca no {@link BeanRegistry}, a verificação
 * de escopos no {@link ScopeManager}, a detecção de dependências circulares e a
 * instanciação através do {@link InstanceCreator}.</p>
 *
 * <p>Oferece suporte à resolução por tipo simples, por nome (qualifier), por coleções
 * ({@code List}, {@code Set}) e por {@code Optional}. Caso uma classe concreta não
 * tenha sido registrada previamente pelo scanner, o resolvedor tenta realizar um
 * registro dinâmico (on-the-fly) antes de falhar.</p>
 *
 * <p>O design é desacoplado da fase de extração de metadados, dependendo exclusivamente
 * dos registros preenchidos durante o processo de escaneamento do framework.</p>
 *
 * @version 4.0
 */
public final class DependencyResolver {

    private BeanRegistry beanRegistry;
    private ScopeManager scopeManager;
    private InstanceCreator instanceCreator;
    private LifecycleManager lifecycleManager;
    private LifecycleEventPublisher eventPublisher;
    private CircularDependencyDetector dependencyDetector;

    public DependencyResolver() {}

    public DependencyResolver(BeanRegistry beanRegistry, ScopeManager scopeManager,
                              InstanceCreator instanceCreator, LifecycleManager lifecycleManager,
                              LifecycleEventPublisher eventPublisher,
                              CircularDependencyDetector dependencyDetector) {
        this.beanRegistry = beanRegistry;
        this.scopeManager = scopeManager;
        this.instanceCreator = instanceCreator;
        this.lifecycleManager = lifecycleManager;
        this.eventPublisher = eventPublisher;
        this.dependencyDetector = dependencyDetector;
    }

    public void setComponentRegistry(BeanRegistry beanRegistry) {
        this.beanRegistry = beanRegistry;
    }

    public void setScopeManager(ScopeManager scopeManager) {
        this.scopeManager = scopeManager;
    }

    public void setInstanceCreator(InstanceCreator instanceCreator) {
        this.instanceCreator = instanceCreator;
    }

    public void setLifecycleManager(LifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }

    public void setEventPublisher(LifecycleEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void setCircularDependencyDetector(CircularDependencyDetector dependencyDetector) {
        this.dependencyDetector = dependencyDetector;
    }

    // =============================================
    // API PÚBLICA
    // =============================================

    /**
     * Recupera um bean gerenciado pelo seu tipo de classe.
     *
     * @param <T>  O tipo do bean.
     * @param type A classe do bean desejado.
     * @return A instância gerenciada do bean.
     * @throws BeanNotFoundException Se nenhuma implementação for encontrada.
     * @throws CircularDependencyException Se uma dependência circular for detectada.
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        return (T) resolve(type, null);
    }

    /**
     * Recupera um bean gerenciado pelo seu tipo e por um qualificador específico.
     *
     * @param <T>       O tipo do bean.
     * @param type      A classe do bean desejado.
     * @param qualifier O nome do qualificador ({@code @Qualifier}).
     * @return A instância gerenciada do bean.
     * @throws BeanNotFoundException Se nenhuma implementação correspondente ao qualificador for encontrada.
     * @throws CircularDependencyException Se uma dependência circular for detectada.
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type, String qualifier) {
        return (T) resolve(type, qualifier);
    }

    /**
     * Recupera um bean gerenciado exclusivamente pelo seu nome lógico registrado.
     *
     * @param <T>  O tipo do bean.
     * @param name O nome lógico do bean.
     * @return A instância gerenciada do bean.
     * @throws BeanNotFoundException Se nenhum bean com o nome fornecido existir.
     * @throws CircularDependencyException Se uma dependência circular for detectada.
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition def = beanRegistry.getDefinition(name);
        if (def == null) throw new BeanNotFoundException("Bean não encontrado: " + name);
        return (T) resolve(def.getType(), null);
    }

    /**
     * Recupera um bean gerenciado combinando nome e tipo.
     *
     * @param <T>  O tipo do bean.
     * @param name O nome lógico do bean.
     * @param type A classe do bean desejado.
     * @return A instância gerenciada do bean.
     * @throws BeanNotFoundException Se não for encontrado ou se houver incompatibilidade de tipo.
     * @throws CircularDependencyException Se uma dependência circular for detectada.
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> type) {
        return (T) resolve(type, name);
    }

    /**
     * Recupera todas as instâncias gerenciadas que implementam ou são subtipos
     * de um determinado tipo de classe.
     *
     * @param <T>  O tipo base.
     * @param type A classe ou interface base.
     * @return Uma lista contendo todas as instâncias encontradas. Retorna uma lista vazia se nenhuma for encontrada.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getAllBeansOfType(Class<T> type) {
        List<BeanDefinition> definitions = beanRegistry.getAllDefinitionsOfType(type);
        if (definitions.isEmpty()) return Collections.emptyList();
        return definitions.stream()
                .map(def -> (T) resolve(def.getType(), null))
                .collect(Collectors.toList());
    }

    /**
     * Método central de resolução que analisa o tipo genérico solicitado e delega
     * para a estratégia correta (simples, coleção ou opcional).
     *
     * @param dependencyType O tipo da dependência (pode ser genérico, como {@code List<MyBean>}).
     * @param qualifierName  O nome do qualificador, ou nulo.
     * @return A instância da dependência resolvida.
     */
    @SuppressWarnings("unchecked")
    public Object resolve(Type dependencyType, String qualifierName) {
        Class<?> rawType = extractRawType(dependencyType);

        if (rawType.equals(List.class) || rawType.equals(Set.class)) {
            if (!(dependencyType instanceof ParameterizedType pt))
                throw new IllegalArgumentException(
                        "Coleção deve ser parametrizada: " + dependencyType.getTypeName());
            return resolveCollection(pt, rawType);
        }

        if (rawType.equals(Optional.class)) {
            if (!(dependencyType instanceof ParameterizedType pt)) return Optional.empty();
            Class<?> innerType = (Class<?>) pt.getActualTypeArguments()[0];
            try {
                return Optional.of(resolveAndCast(innerType, qualifierName));
            } catch (BeanNotFoundException e) {
                return Optional.empty();
            }
        }

        return resolveAndCast(rawType, qualifierName);
    }

    /**
     * Sobrecarga de resolução sem qualificador.
     *
     * @param <T>  O tipo da dependência.
     * @param type O tipo da dependência.
     * @return A instância da dependência resolvida.
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(Type type) {
        return (T) resolve(type, null);
    }

    // =============================================
    // RESOLUÇÃO PRINCIPAL
    // =============================================

    /**
     * Executa a resolução efetiva de um bean, aplicando controle de escopo,
     * verificação de dependência circular e instanciação via AOT ou estratégia padrão.
     *
     * @param type          O tipo da classe a ser resolvida.
     * @param qualifierName O qualificador opcional.
     * @return A instância do bean.
     * @throws CircularDependencyException Se o tipo já estiver na pilha de resolução da thread atual.
     * @throws BeanNotFoundException Se a definição não puder ser encontrada ou criada.
     */
    private Object resolveAndCast(Class<?> type, String qualifierName) {
        if (dependencyDetector.isResolving(type))
            throw new CircularDependencyException(
                    "Dependência circular detetada: " + type.getName());

        dependencyDetector.startResolution(type);
        try {
            BeanDefinition definition = findDefinition(type, qualifierName);
            if (definition == null)
                throw new BeanNotFoundException(
                        "Nenhum componente registado para: " + type.getName());

            final Class<?> implType = definition.getType();
            ScopeInterface scope = scopeManager.getScopeHandler(
                    definition.getScopeType().getName());

            @SuppressWarnings({"unchecked", "rawtypes"})
            Object result = scope.get((Class) implType, () -> {
                InstanceFactory<?> aotFactory = beanRegistry.getAotFactory(implType);
                if (aotFactory != null) {
                    return aotFactory.create(this);
                }
                if (definition.isFactoryMethod())
                    return createFromFactoryMethod(definition);
                return instanceCreator.createInstance(implType);
            });

            if (eventPublisher != null) {
                eventPublisher.publishEvent(type, qualifierName,
                        DependencyLifecycleListener.LifecycleEventType.AFTER_POST_CONSTRUCT,
                        result);
            }
            return result;
        } finally {
            dependencyDetector.endResolution(type);
        }
    }

    // =============================================
    // BUSCA DE DEFINIÇÃO
    // =============================================

    /**
     * Localiza a definição de bean mais apropriada com base no tipo e no qualificador.
     *
     * <p>Se nenhum bean for encontrado e o tipo solicitado for uma classe concreta
     * (não abstrata ou interface), o método tenta gerar e registrar uma definição
     * mínima dinamicamente antes de retornar nulo.</p>
     *
     * @param type          O tipo alvo da busca.
     * @param qualifierName O nome do qualificador (pode ser nulo ou vazio).
     * @return A definição do bean encontrada, ou nulo se irrecuperável.
     * @throws BeanNotFoundException Se o qualificador for especificado mas não encontrado.
     * @throws DependencyNotRegisteredException Se houver múltiplas implementações e nenhuma forma de desambiguar.
     */
    private BeanDefinition findDefinition(Class<?> type, String qualifierName) {
        if (qualifierName != null && !qualifierName.isEmpty()) {
            BeanDefinition def = beanRegistry.getDefinition(qualifierName);
            if (def != null && type.isAssignableFrom(def.getType())) return def;
            throw new BeanNotFoundException(
                    "Qualifier '" + qualifierName + "' não encontrado para: " + type.getName());
        }

        BeanDefinition def = beanRegistry.getDefinition(type);
        if (def != null) return def;

        List<BeanDefinition> all = beanRegistry.getAllDefinitionsOfType(type);
        if (all.size() == 1) return all.get(0);
        if (all.size() > 1) throw new DependencyNotRegisteredException(
                "Múltiplas implementações para " + type.getName() +
                        ". Use @Primary ou @Qualifier.");

        if (!type.isInterface() && !java.lang.reflect.Modifier.isAbstract(type.getModifiers())) {
            String name = Character.toLowerCase(type.getSimpleName().charAt(0))
                    + type.getSimpleName().substring(1);
            BeanDefinition newDef = new BeanDefinition(
                    name, type, ScopeType.SINGLETON,
                    Collections.emptyList(), null, null,
                    false, null, Collections.emptyMap());
            beanRegistry.registerDefinition(newDef);
            return newDef;
        }
        return null;
    }

    // =============================================
    // FACTORY METHOD
    // =============================================

    /**
     * Cria uma instância de bean invocando um método fábrica ({@code @Bean}).
     *
     * <p>Após a invocação do método fábrica, o bean resultante passa pelos processos
     * de injeção de dependências e {@code @PostConstruct}.</p>
     *
     * @param definition A definição contendo os metadados do método fábrica.
     * @return A instância criada pelo método fábrica.
     * @throws RuntimeException Se a invocação do método falhar.
     */
    @SuppressWarnings("unchecked")
    private Object createFromFactoryMethod(BeanDefinition definition) {
        Class<?> factoryClass = definition.getFactoryClass();
        java.lang.reflect.Method factoryMethod = definition.getFactoryMethod();
        Object factoryInstance = resolveAndCast(factoryClass, null);
        try {
            Object[] args = resolveParameters(factoryMethod);
            factoryMethod.setAccessible(true);
            Object instance = factoryMethod.invoke(factoryInstance, args);
            instanceCreator.injectAndPostConstruct(instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Falha no @Bean " + factoryMethod.getName() +
                            " de " + factoryClass.getName(), e);
        }
    }

    /**
     * Resolve os argumentos necessários para a execução de um método fábrica.
     *
     * @param method O método cujos parâmetros serão resolvidos.
     * @return Um array de dependências resolvidas.
     */
    private Object[] resolveParameters(java.lang.reflect.Method method) {
        java.lang.reflect.Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++)
            args[i] = resolve(params[i].getParameterizedType(), getQualifierValue(params[i]));
        return args;
    }

    // =============================================
    // COLEÇÕES
    // =============================================

    /**
     * Resolve uma injeção de dependência do tipo coleção ({@code List} ou {@code Set}).
     *
     * <p>Busca todas as definições correspondentes ao tipo genérico da coleção,
     * instancia cada uma e retorna a agregação. Beans que não puderem ser resolvidos
     * são silenciosamente ignorados na coleção resultante.</p>
     *
     * @param dependencyType O tipo genérico completo da coleção.
     * @param collectionType O tipo bruto da coleção ({@code List.class} ou {@code Set.class}).
     * @return Uma lista ou conjunto contendo as instâncias resolvedas.
     * @throws IllegalArgumentException Se o tipo interno da coleção não for uma classe concreta.
     */
    @SuppressWarnings("unchecked")
    private Object resolveCollection(ParameterizedType dependencyType, Class<?> collectionType) {
        Type innerType = dependencyType.getActualTypeArguments()[0];
        if (!(innerType instanceof Class<?> componentType))
            throw new IllegalArgumentException("Coleção requer tipo concreto.");

        List<Object> instances = beanRegistry.getAllDefinitionsOfType(componentType).stream()
                .map(def -> {
                    try {
                        return resolveAndCast(def.getType(), def.getName());
                    } catch (BeanNotFoundException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return collectionType.equals(Set.class) ? new HashSet<>(instances) : instances;
    }

    // =============================================
    // UTILITÁRIOS
    // =============================================

    /**
     * Extrai o tipo bruto (Raw Type) de um objeto {@link Type}, removendo
     * informações de parametrização genérica.
     *
     * @param type O tipo a ser analisado.
     * @return A classe crua correspondente.
     * @throws IllegalArgumentException Se o tipo fornecido não for suportado.
     */
    private Class<?> extractRawType(Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pt) return (Class<?>) pt.getRawType();
        throw new IllegalArgumentException("Tipo não suportado: " + type.getTypeName());
    }

    /**
     * Extrai o valor da anotação {@code @Qualifier} presente em um parâmetro de método.
     *
     * @param param O parâmetro a ser verificado.
     * @return O nome do qualificador, ou nulo se ausente ou vazio.
     */
    private String getQualifierValue(java.lang.reflect.Parameter param) {
        Qualifier qualifier = param.getAnnotation(Qualifier.class);
        return (qualifier != null && !qualifier.value().isEmpty()) ? qualifier.value() : null;
    }
}