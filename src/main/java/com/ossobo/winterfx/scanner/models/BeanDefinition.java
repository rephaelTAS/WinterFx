package com.ossobo.winterfx.scanner.models;

import com.ossobo.winterfx.di.scopes.enums.ScopeType;

import java.lang.reflect.Method;
import java.util.*;
import java.util.Objects;

/**
 * Representa a definição de um Bean no container de injeção de dependências.
 *
 * <p>Armazena todas as informações necessárias para que o {@code DiContainer}
 * possa instanciar, injetar dependências e gerenciar o ciclo de vida do bean.</p>
 *
 * <p>Suporta dois modos de definição:</p>
 * <ul>
 *   <li><b>Component Scanning:</b> beans descobertos via {@code @Component}, {@code @Service}, etc.</li>
 *   <li><b>Factory Method:</b> beans definidos via {@code @Configuration} + {@code @Bean}</li>
 * </ul>
 *
 * <p>Contém metadados para:</p>
 * <ul>
 *   <li>Injeção de dependências ({@code @Inject})</li>
 *   <li>Ciclo de vida ({@code @PostConstruct}, {@code @PreDestroy})</li>
 *   <li>Escopo ({@code @Scope})</li>
 *   <li>Seleção de beans ({@code @Primary}, {@code @Qualifier})</li>
 *   <li>Injeção de propriedades ({@code @Value})</li>
 * </ul>
 *
 * @see ScopeType
 * @see InjectionPoint
 */
public class BeanDefinition {

    private final String name;
    private final Class<?> type;
    private final ScopeType scopeType;
    private final Class<?> factoryClass;
    private final Method factoryMethod;
    private final List<InjectionPoint> dependencies;
    private final Method postConstructMethod;
    private final Method preDestroyMethod;
    private final boolean primary;
    private final String qualifier;
    private final Map<String, String> values;

    /**
     * Construtor para beans definidos por Component Scanning.
     *
     * @param name nome do bean
     * @param type tipo do bean (classe)
     * @param scopeType escopo do bean (singleton, prototype, etc.)
     * @param dependencies lista de pontos de injeção
     * @param postConstructMethod método de inicialização (null se não houver)
     * @param preDestroyMethod método de destruição (null se não houver)
     * @param primary true se o bean é marcado com @Primary
     * @param qualifier valor do @Qualifier no TYPE, ou null
     * @param values mapa de fieldName → expressão (@Value)
     */
    public BeanDefinition(String name, Class<?> type, ScopeType scopeType,
                          List<InjectionPoint> dependencies,
                          Method postConstructMethod, Method preDestroyMethod,
                          boolean primary, String qualifier, Map<String, String> values) {
        this.name = Objects.requireNonNull(name, "name não pode ser nulo");
        this.type = Objects.requireNonNull(type, "type não pode ser nulo");
        this.scopeType = Objects.requireNonNull(scopeType, "scopeType não pode ser nulo");
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies não pode ser nulo");
        this.factoryClass = null;
        this.factoryMethod = null;
        this.postConstructMethod = postConstructMethod;
        this.preDestroyMethod = preDestroyMethod;
        this.primary = primary;
        this.qualifier = qualifier;
        this.values = values != null ? Collections.unmodifiableMap(values) : Collections.emptyMap();
    }

    /**
     * Construtor para beans definidos por Factory Method (@Configuration + @Bean).
     *
     * @param name nome do bean
     * @param type tipo do bean (retornado pelo factory method)
     * @param scopeType escopo do bean (singleton, prototype, etc.)
     * @param factoryClass classe contendo o factory method
     * @param factoryMethod método factory que cria o bean
     * @param dependencies lista de pontos de injeção
     * @param postConstructMethod método de inicialização (null se não houver)
     * @param preDestroyMethod método de destruição (null se não houver)
     * @param primary true se o bean é marcado com @Primary
     * @param qualifier valor do @Qualifier no TYPE, ou null
     * @param values mapa de fieldName → expressão (@Value)
     */
    public BeanDefinition(String name, Class<?> type, ScopeType scopeType,
                          Class<?> factoryClass, Method factoryMethod,
                          List<InjectionPoint> dependencies,
                          Method postConstructMethod, Method preDestroyMethod,
                          boolean primary, String qualifier, Map<String, String> values) {
        this.name = Objects.requireNonNull(name, "name não pode ser nulo");
        this.type = Objects.requireNonNull(type, "type não pode ser nulo");
        this.scopeType = Objects.requireNonNull(scopeType, "scopeType não pode ser nulo");
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies não pode ser nulo");
        this.factoryClass = Objects.requireNonNull(factoryClass, "factoryClass não pode ser nulo");
        this.factoryMethod = Objects.requireNonNull(factoryMethod, "factoryMethod não pode ser nulo");
        this.postConstructMethod = postConstructMethod;
        this.preDestroyMethod = preDestroyMethod;
        this.primary = primary;
        this.qualifier = qualifier;
        this.values = values != null ? Collections.unmodifiableMap(values) : Collections.emptyMap();
    }

    // ===== GETTERS =====

    public String getName() { return name; }
    public Class<?> getType() { return type; }
    public ScopeType getScopeType() { return scopeType; }
    public Class<?> getFactoryClass() { return factoryClass; }
    public Method getFactoryMethod() { return factoryMethod; }
    public boolean isFactoryMethod() { return factoryMethod != null; }
    public List<InjectionPoint> getDependencies() { return dependencies; }
    public Method getPostConstructMethod() { return postConstructMethod; }
    public Method getPreDestroyMethod() { return preDestroyMethod; }
    public boolean isPrimary() { return primary; }
    public String getQualifier() { return qualifier; }
    public Map<String, String> getValues() { return values; }

    /**
     * @return true se o bean usa @Scope personalizado
     */
    public boolean hasCustomScope() {
        return scopeType != ScopeType.SINGLETON;
    }

    /**
     * @return lista de nomes de campos com @Value
     */
    public List<String> getValueFieldNames() {
        return Collections.unmodifiableList(new ArrayList<>(values.keySet()));
    }

    /**
     * Retorna a expressão @Value para um campo específico.
     *
     * @param fieldName nome do campo
     * @return expressão @Value, ou null se o campo não tiver @Value
     */
    public String getValueExpression(String fieldName) {
        return values.get(fieldName);
    }

    @Override
    public String toString() {
        String source = isFactoryMethod()
                ? "Factory: " + factoryClass.getSimpleName() + "." + factoryMethod.getName() + "()"
                : "Class: " + type.getName();

        StringBuilder sb = new StringBuilder();
        sb.append("BeanDefinition[name=").append(name)
                .append(", type=").append(type.getSimpleName())
                .append(", scope=").append(scopeType.name())
                .append(", primary=").append(primary);

        if (qualifier != null) {
            sb.append(", qualifier=").append(qualifier);
        }

        if (hasCustomScope()) {
            sb.append(", customScope=true");
        }

        sb.append(", source=").append(source)
                .append(", dependencies=").append(dependencies.size())
                .append(", valueFields=").append(values.size())
                .append("]");

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BeanDefinition that = (BeanDefinition) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}