package com.ossobo.winterfx.scanner;

import com.ossobo.winterfx.anotations.*;
import com.ossobo.winterfx.di.annotations.*;
import com.ossobo.winterfx.di.scopes.enums.ScopeType;
import com.ossobo.winterfx.scanner.models.BeanDefinition;      // 🆕 Import adicionado
import com.ossobo.winterfx.scanner.models.InjectionPoint;     // 🆕 Import adicionado
import com.ossobo.winterfx.scanner.registry.BeanRegistry;     // 🆕 Import adicionado

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 🏷️ BeanAnnotationScanner
 *
 * Responsabilidade: Encontrar e registrar BEANS.
 * Busca por: @Component, @Service, @Repository, @Controller, @Configuration
 */
public final class BeanAnnotationScanner {

    private static final Logger LOGGER = Logger.getLogger(BeanAnnotationScanner.class.getName());

    private static final Set<Class<? extends Annotation>> BEAN_ANNOTATIONS =
            Set.of(Component.class, Service.class, Repository.class, Controller.class);

    private final BeanMetadataExtractor metadataExtractor;
    private final String[] basePackages;

    private int componentsFound = 0;
    private int configurationsFound = 0;

    public BeanAnnotationScanner(String... basePackages) {
        this.basePackages = (basePackages != null && basePackages.length > 0)
                ? basePackages : new String[]{""};
        this.metadataExtractor = new BeanMetadataExtractor(new ReflectionScanner());
    }

    /**
     * Executa scan + registro de beans.
     */
    public int scanAndRegister(BeanRegistry registry) {  // 🔄 ComponentRegistry → BeanRegistry
        LOGGER.log(Level.INFO, "🏷️ Iniciando scan de beans nos pacotes: {0}",
                String.join(", ", basePackages));
        long startTime = System.currentTimeMillis();

        try (ScanResult result = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .acceptPackages(basePackages)
                .scan()) {

            // FASE 1: Componentes
            scanComponents(result, registry);

            // FASE 2: @Configuration
            scanConfigurations(result, registry);

            long duration = System.currentTimeMillis() - startTime;
            int total = componentsFound + configurationsFound;
            LOGGER.log(Level.INFO, "✅ Beans registrados: {0} ({1}ms)", new Object[]{total, duration});

            return total;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Falha no scan de beans: {0}", e.getMessage());
            throw new RuntimeException("Falha no escaneamento de beans", e);
        }
    }

    // ===== COMPONENTES =====

    private void scanComponents(ScanResult result, BeanRegistry registry) {  // 🔄 ComponentRegistry → BeanRegistry
        Set<String> names = new HashSet<>();
        for (Class<? extends Annotation> ann : BEAN_ANNOTATIONS) {
            names.addAll(result.getClassesWithAnnotation(ann).getNames());
        }

        LOGGER.log(Level.INFO, "📦 Componentes encontrados: {0}", names.size());

        for (String className : names) {
            try {
                Class<?> type = Class.forName(className);
                registerComponent(type, registry);
                componentsFound++;
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "⚠️ Classe não encontrada: {0}", className);
            }
        }
    }

    private void registerComponent(Class<?> type, BeanRegistry registry) {  // 🔄 ComponentRegistry → BeanRegistry
        String beanName = getBeanName(type);
        ScopeType scopeType = determineScope(type);

        List<InjectionPoint> dependencies = metadataExtractor.extractInjectionPoints(type);
        Method postConstruct = metadataExtractor.extractPostConstruct(type);
        Method preDestroy = metadataExtractor.extractPreDestroy(type);

        BeanDefinition definition = new BeanDefinition(
                beanName, type, scopeType,
                dependencies, postConstruct, preDestroy
        );
        registry.registerDefinition(definition);

        LOGGER.log(Level.FINE, "✅ Registado: {0}", definition);
    }

    // ===== CONFIGURATION =====

    private void scanConfigurations(ScanResult result, BeanRegistry registry) {  // 🔄 ComponentRegistry → BeanRegistry
        List<String> configNames = result.getClassesWithAnnotation(Configuration.class).getNames();
        LOGGER.log(Level.INFO, "⚙️ @Configuration encontradas: {0}", configNames.size());

        for (String className : configNames) {
            try {
                Class<?> type = Class.forName(className);
                if (type.isAnnotationPresent(Configuration.class)) {
                    registerConfiguration(type, registry);
                    configurationsFound++;
                }
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "⚠️ @Configuration não encontrada: {0}", className);
            }
        }
    }

    private void registerConfiguration(Class<?> configClass, BeanRegistry registry) {  // 🔄 ComponentRegistry → BeanRegistry
        String beanName = getConfigBeanName(configClass);
        ScopeType scopeType = determineScope(configClass);

        List<InjectionPoint> dependencies = metadataExtractor.extractInjectionPoints(configClass);
        Method postConstruct = metadataExtractor.extractPostConstruct(configClass);
        Method preDestroy = metadataExtractor.extractPreDestroy(configClass);

        BeanDefinition definition = new BeanDefinition(
                beanName, configClass, scopeType,
                dependencies, postConstruct, preDestroy
        );
        registry.registerDefinition(definition);

        // Processa @Bean methods
        for (Method method : configClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Bean.class)) {
                registerFactoryBean(configClass, method, registry);
            }
        }
    }

    private void registerFactoryBean(Class<?> factoryClass, Method factoryMethod, BeanRegistry registry) {  // 🔄 ComponentRegistry → BeanRegistry
        Bean bean = factoryMethod.getAnnotation(Bean.class);
        String beanName = bean.name().isEmpty() ? factoryMethod.getName() : bean.name();
        Class<?> beanType = factoryMethod.getReturnType();
        ScopeType scopeType = determineScope(factoryClass);

        List<InjectionPoint> dependencies = metadataExtractor.extractInjectionPoints(beanType);
        Method postConstruct = metadataExtractor.extractPostConstruct(beanType);
        Method preDestroy = metadataExtractor.extractPreDestroy(beanType);

        BeanDefinition definition = new BeanDefinition(
                beanName, beanType, scopeType,
                factoryClass, factoryMethod,
                dependencies, postConstruct, preDestroy
        );
        registry.registerDefinition(definition);
    }

    // ===== UTILITÁRIOS =====

    private String getBeanName(Class<?> type) {
        for (Class<? extends Annotation> ann : BEAN_ANNOTATIONS) {
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

    private String getConfigBeanName(Class<?> configClass) {
        Configuration config = configClass.getAnnotation(Configuration.class);
        String value = config.value();
        return value.isEmpty() ? getDefaultBeanName(configClass) : value;
    }

    private String getDefaultBeanName(Class<?> type) {
        return Character.toLowerCase(type.getSimpleName().charAt(0))
                + type.getSimpleName().substring(1);
    }

    private ScopeType determineScope(Class<?> type) {
        if (type.isAnnotationPresent(Scope.class)) {
            return type.getAnnotation(Scope.class).value();
        }
        return ScopeType.SINGLETON;
    }

    public int getComponentsFound() { return componentsFound; }
    public int getConfigurationsFound() { return configurationsFound; }
    public int getTotalFound() { return componentsFound + configurationsFound; }
}