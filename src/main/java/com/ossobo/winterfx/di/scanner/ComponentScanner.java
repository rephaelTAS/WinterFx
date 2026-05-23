package com.ossobo.winterfx.di.scanner;

import com.ossobo.winterfx.di.scanner.models.BeanDefinition;
import com.ossobo.winterfx.di.annotations.*;
import com.ossobo.winterfx.di.scopes.enums.ScopeType;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ComponentScanner v2.0
 *
 * Responsabilidade única: descobrir classes anotadas via ClassGraph.
 *
 * Fluxo:
 * 1. ClassGraph scan → nomes de classes (NÃO carrega)
 * 2. Para cada nome → Class.forName (carrega sob demanda)
 * 3. Regista no ComponentRegistry
 *
 * Suporta:
 * - @Component, @Service, @Repository, @Controller
 * - @Configuration com métodos @Bean
 * - @ComponentScan para pacotes adicionais
 *
 * @since 2.0
 */
public final class ComponentScanner {

    private static final Logger LOGGER = Logger.getLogger(ComponentScanner.class.getName());

    private static final Set<Class<? extends Annotation>> COMPONENT_ANNOTATIONS =
            Set.of(Component.class, Service.class, Repository.class, Controller.class);

    private final ComponentRegistry registry;
    private final String[] basePackages;

    public ComponentScanner(ComponentRegistry registry, String... basePackages) {
        this.registry = registry;
        this.basePackages = (basePackages != null && basePackages.length > 0)
                ? basePackages
                : new String[]{""};
    }

    /**
     * Executa o scan completo:
     * 1. ClassGraph descobre nomes de classes
     * 2. Carrega cada classe e regista no ComponentRegistry
     */
    public void scanAndRegister() {
        LOGGER.log(Level.INFO, "Iniciando scan nos pacotes: {0}", String.join(", ", basePackages));

        try (ScanResult result = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .acceptPackages(basePackages)
                .scan()) {

            // === COMPONENTES ===
            Set<String> componentNames = new HashSet<>();
            for (Class<? extends Annotation> ann : COMPONENT_ANNOTATIONS) {
                componentNames.addAll(result.getClassesWithAnnotation(ann).getNames());
            }

            LOGGER.log(Level.INFO, "ClassGraph encontrou {0} componentes.", componentNames.size());

            for (String className : componentNames) {
                registerComponentByName(className);
            }

            // === @Configuration ===
            List<String> configNames = result.getClassesWithAnnotation(Configuration.class).getNames();
            LOGGER.log(Level.INFO, "ClassGraph encontrou {0} @Configuration.", configNames.size());

            for (String className : configNames) {
                processConfigurationByName(className);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Falha no scan: {0}", e.getMessage());
            throw new RuntimeException("Falha no escaneamento de componentes", e);
        }

        LOGGER.log(Level.INFO, "Scan concluído. Total de beans: {0}", registry.getBeanNames().size());
    }

    /**
     * Scan adicional para @ComponentScan.
     */
    public void scanAdditional(String... additionalPackages) {
        if (additionalPackages == null || additionalPackages.length == 0) return;

        LOGGER.log(Level.INFO, "@ComponentScan: pacotes adicionais: {0}",
                String.join(", ", additionalPackages));

        try (ScanResult result = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .acceptPackages(additionalPackages)
                .scan()) {

            Set<String> componentNames = new HashSet<>();
            for (Class<? extends Annotation> ann : COMPONENT_ANNOTATIONS) {
                componentNames.addAll(result.getClassesWithAnnotation(ann).getNames());
            }

            for (String className : componentNames) {
                registerComponentByName(className);
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Falha no scan adicional: {0}", e.getMessage());
        }
    }

    // ===== REGISTO A PARTIR DO NOME (ClassGraph → Class.forName) =====

    private void registerComponentByName(String className) {
        try {
            Class<?> type = Class.forName(className);
            if (isComponent(type)) {
                registerComponent(type);
            }
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "Classe não encontrada: {0}", className);
        }
    }

    private void processConfigurationByName(String className) {
        try {
            Class<?> type = Class.forName(className);
            if (type.isAnnotationPresent(Configuration.class)) {
                processConfigurationClass(type);
            }
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "@Configuration não encontrada: {0}", className);
        }
    }

    // ===== REGISTO DE COMPONENTES =====

    private boolean isComponent(Class<?> type) {
        return COMPONENT_ANNOTATIONS.stream().anyMatch(type::isAnnotationPresent);
    }

    private void registerComponent(Class<?> type) {
        String beanName = getBeanName(type);
        ScopeType scopeType = determineScope(type);

        BeanDefinition definition = new BeanDefinition(beanName, type, scopeType);
        registry.registerDefinition(definition);

        LOGGER.log(Level.FINE, "Registado: {0}", definition);
    }

    private String getBeanName(Class<?> type) {
        for (Class<? extends Annotation> ann : COMPONENT_ANNOTATIONS) {
            if (type.isAnnotationPresent(ann)) {
                try {
                    Method valueMethod = ann.getMethod("value");
                    String value = (String) valueMethod.invoke(type.getAnnotation(ann));
                    if (!value.isEmpty()) return value;
                } catch (Exception ignored) {}
            }
        }
        return Character.toLowerCase(type.getSimpleName().charAt(0))
                + type.getSimpleName().substring(1);
    }

    private ScopeType determineScope(Class<?> type) {
        if (type.isAnnotationPresent(ScopeAnnotation.class)) {
            ScopeType scopeValue = type.getAnnotation(ScopeAnnotation.class).value();
            return scopeValue;
        }
        return ScopeType.SINGLETON;
    }

    // ===== @Configuration + @Bean =====

    private void processConfigurationClass(Class<?> configClass) {
        String beanName = getConfigBeanName(configClass);
        ScopeType scopeType = determineScope(configClass);

        registry.registerDefinition(new BeanDefinition(beanName, configClass, scopeType));

        // Verifica @ComponentScan
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan scan = configClass.getAnnotation(ComponentScan.class);
            scanAdditional(scan.value());
        }

        // Processa métodos @Bean
        for (Method method : configClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Bean.class)) {
                registerFactoryBean(configClass, method);
            }
        }
    }

    private String getConfigBeanName(Class<?> configClass) {
        Configuration config = configClass.getAnnotation(Configuration.class);
        String value = config.value();
        return value.isEmpty() ? getDefaultBeanName(configClass) : value;
    }

    private void registerFactoryBean(Class<?> factoryClass, Method factoryMethod) {
        Bean bean = factoryMethod.getAnnotation(Bean.class);
        String beanName = bean.name().isEmpty() ? factoryMethod.getName() : bean.name();
        Class<?> beanType = factoryMethod.getReturnType();
        ScopeType scopeType = determineScope(factoryClass);

        BeanDefinition definition = new BeanDefinition(
                beanName, beanType, scopeType, factoryClass, factoryMethod
        );
        registry.registerDefinition(definition);

        LOGGER.log(Level.FINE, "Registado @Bean: {0}", definition);
    }

    private String getDefaultBeanName(Class<?> type) {
        return Character.toLowerCase(type.getSimpleName().charAt(0))
                + type.getSimpleName().substring(1);
    }
}

package com.ossobo.winterfx.di.scanner;

import com.ossobo.winterfx.di.scanner.models.BeanDefinition;
import com.ossobo.winterfx.di.annotations.*;
        import com.ossobo.winterfx.di.scopes.enums.ScopeType;
import com.ossobo.winterfx.resources.api.ResourceAPI;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
        import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ComponentScanner v2.1
 *
 * Scanner principal que orquestra:
 * - Componentes (@Component, @Service, @Repository, @Controller)
 * - Configurações (@Configuration + @Bean)
 * - Recursos (@RegisterView, @RegisterImage) ← NOVO!
 *
 * @since 2.1
 */
public final class ComponentScanner {

    private static final Logger LOGGER = Logger.getLogger(ComponentScanner.class.getName());

    private static final Set<Class<? extends Annotation>> COMPONENT_ANNOTATIONS =
            Set.of(Component.class, Service.class, Repository.class, Controller.class);

    private final ComponentRegistry registry;
    private final String[] basePackages;
    private final ResourceAPI resourceAPI;
    private final boolean scanResources;

    /**
     * Construtor completo.
     *
     * @param registry      Registry de componentes
     * @param resourceAPI   API de recursos (pode ser null se scanResources=false)
     * @param scanResources Se true, também escaneia @RegisterView e @RegisterImage
     * @param basePackages  Pacotes a escanear
     */
    public ComponentScanner(ComponentRegistry registry,
                            ResourceAPI resourceAPI,
                            boolean scanResources,
                            String... basePackages) {
        this.registry = registry;
        this.resourceAPI = resourceAPI;
        this.scanResources = scanResources;
        this.basePackages = (basePackages != null && basePackages.length > 0)
                ? basePackages
                : new String[]{""};
    }

    /**
     * Construtor sem ResourceAPI (apenas componentes).
     */
    public ComponentScanner(ComponentRegistry registry, String... basePackages) {
        this(registry, null, false, basePackages);
    }

    /**
     * Executa o scan completo:
     * 1. Componentes (@Component, @Service, etc.)
     * 2. @Configuration + @Bean
     * 3. @RegisterView e @RegisterImage (se scanResources=true) ← NOVO!
     */
    public void scanAndRegister() {
        LOGGER.log(Level.INFO, "🔍 Iniciando scan completo nos pacotes: {0}",
                String.join(", ", basePackages));

        long startTime = System.currentTimeMillis();

        try (ScanResult result = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .acceptPackages(basePackages)
                .scan()) {

            // === FASE 1: COMPONENTES ===
            int componentCount = scanComponents(result);

            // === FASE 2: CONFIGURATION ===
            int configCount = scanConfigurations(result);

            // === FASE 3: RECURSOS (NOVO!) ===
            int resourceCount = 0;
            if (scanResources && resourceAPI != null) {
                resourceCount = scanResources(result);
            }

            long duration = System.currentTimeMillis() - startTime;
            printSummary(duration, componentCount, configCount, resourceCount);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Falha no scan: {0}", e.getMessage());
            throw new RuntimeException("Falha no escaneamento", e);
        }
    }

    // ===== FASE 1: COMPONENTES =====

    private int scanComponents(ScanResult result) {
        Set<String> componentNames = new HashSet<>();
        for (Class<? extends Annotation> ann : COMPONENT_ANNOTATIONS) {
            componentNames.addAll(result.getClassesWithAnnotation(ann).getNames());
        }

        LOGGER.log(Level.INFO, "📦 Componentes encontrados: {0}", componentNames.size());

        for (String className : componentNames) {
            registerComponentByName(className);
        }

        return componentNames.size();
    }

    // ===== FASE 2: CONFIGURATION =====

    private int scanConfigurations(ScanResult result) {
        List<String> configNames = result.getClassesWithAnnotation(Configuration.class).getNames();
        LOGGER.log(Level.INFO, "⚙️ @Configuration encontradas: {0}", configNames.size());

        for (String className : configNames) {
            processConfigurationByName(className);
        }

        return configNames.size();
    }

    // ===== FASE 3: RECURSOS (NOVO!) =====

    /**
     * Escaneia e registra recursos (@RegisterView, @RegisterImage).
     * Usa o AnnotationScanner internamente.
     */
    private int scanResources(ScanResult result) {
        LOGGER.log(Level.INFO, "🏷️ Escaneando recursos...");

        AnnotationScanner annotationScanner = new AnnotationScanner(resourceAPI, basePackages);
        annotationScanner.scanAndRegister();

        int total = annotationScanner.getTotalResourcesFound();
        LOGGER.log(Level.INFO, "🏷️ Recursos registrados: {0}", total);
        return total;
    }

    /**
     * Scan adicional para @ComponentScan.
     */
    public void scanAdditional(String... additionalPackages) {
        if (additionalPackages == null || additionalPackages.length == 0) return;

        LOGGER.log(Level.INFO, "@ComponentScan: pacotes adicionais: {0}",
                String.join(", ", additionalPackages));

        try (ScanResult result = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .acceptPackages(additionalPackages)
                .scan()) {

            Set<String> componentNames = new HashSet<>();
            for (Class<? extends Annotation> ann : COMPONENT_ANNOTATIONS) {
                componentNames.addAll(result.getClassesWithAnnotation(ann).getNames());
            }

            for (String className : componentNames) {
                registerComponentByName(className);
            }

            // Também escaneia recursos nos pacotes adicionais
            if (scanResources && resourceAPI != null) {
                AnnotationScanner additionalScanner = new AnnotationScanner(resourceAPI, additionalPackages);
                additionalScanner.scanAndRegister();
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Falha no scan adicional: {0}", e.getMessage());
        }
    }

    // ===== REGISTO DE COMPONENTES (mantido igual) =====

    private void registerComponentByName(String className) {
        try {
            Class<?> type = Class.forName(className);
            if (isComponent(type)) {
                registerComponent(type);
            }
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "Classe não encontrada: {0}", className);
        }
    }

    private void processConfigurationByName(String className) {
        try {
            Class<?> type = Class.forName(className);
            if (type.isAnnotationPresent(Configuration.class)) {
                processConfigurationClass(type);
            }
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "@Configuration não encontrada: {0}", className);
        }
    }

    private boolean isComponent(Class<?> type) {
        return COMPONENT_ANNOTATIONS.stream().anyMatch(type::isAnnotationPresent);
    }

    private void registerComponent(Class<?> type) {
        String beanName = getBeanName(type);
        ScopeType scopeType = determineScope(type);

        BeanDefinition definition = new BeanDefinition(beanName, type, scopeType);
        registry.registerDefinition(definition);

        LOGGER.log(Level.FINE, "Registado: {0}", definition);
    }

    private String getBeanName(Class<?> type) {
        for (Class<? extends Annotation> ann : COMPONENT_ANNOTATIONS) {
            if (type.isAnnotationPresent(ann)) {
                try {
                    Method valueMethod = ann.getMethod("value");
                    String value = (String) valueMethod.invoke(type.getAnnotation(ann));
                    if (!value.isEmpty()) return value;
                } catch (Exception ignored) {}
            }
        }
        return Character.toLowerCase(type.getSimpleName().charAt(0))
                + type.getSimpleName().substring(1);
    }

    private ScopeType determineScope(Class<?> type) {
        if (type.isAnnotationPresent(ScopeAnnotation.class)) {
            return type.getAnnotation(ScopeAnnotation.class).value();
        }
        return ScopeType.SINGLETON;
    }

    // ===== @Configuration + @Bean (mantido igual) =====

    private void processConfigurationClass(Class<?> configClass) {
        String beanName = getConfigBeanName(configClass);
        ScopeType scopeType = determineScope(configClass);

        registry.registerDefinition(new BeanDefinition(beanName, configClass, scopeType));

        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan scan = configClass.getAnnotation(ComponentScan.class);
            scanAdditional(scan.value());
        }

        for (Method method : configClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Bean.class)) {
                registerFactoryBean(configClass, method);
            }
        }
    }

    private String getConfigBeanName(Class<?> configClass) {
        Configuration config = configClass.getAnnotation(Configuration.class);
        String value = config.value();
        return value.isEmpty() ? getDefaultBeanName(configClass) : value;
    }

    private void registerFactoryBean(Class<?> factoryClass, Method factoryMethod) {
        Bean bean = factoryMethod.getAnnotation(Bean.class);
        String beanName = bean.name().isEmpty() ? factoryMethod.getName() : bean.name();
        Class<?> beanType = factoryMethod.getReturnType();
        ScopeType scopeType = determineScope(factoryClass);

        BeanDefinition definition = new BeanDefinition(
                beanName, beanType, scopeType, factoryClass, factoryMethod
        );
        registry.registerDefinition(definition);

        LOGGER.log(Level.FINE, "Registado @Bean: {0}", definition);
    }

    private String getDefaultBeanName(Class<?> type) {
        return Character.toLowerCase(type.getSimpleName().charAt(0))
                + type.getSimpleName().substring(1);
    }

    // ===== SUMÁRIO =====

    private void printSummary(long durationMs, int components, int configs, int resources) {
        LOGGER.log(Level.INFO, """

                ═══════════════════════════════════════
                 🔍 SCAN COMPLETO CONCLUÍDO
                ═══════════════════════════════════════
                Duração: {0}ms
                📦 Componentes:    {1}
                ⚙️  Configurations: {2}
                🏷️  Recursos:       {3}
                📊 Total de beans:  {4}
                ═══════════════════════════════════════""",
                new Object[]{durationMs, components, configs, resources,
                        registry.getBeanNames().size()});
    }
}