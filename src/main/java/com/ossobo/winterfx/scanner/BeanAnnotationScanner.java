package com.ossobo.winterfx.scanner;

import com.ossobo.winterfx.anotations.*;
import com.ossobo.winterfx.di.annotations.Configuration;
import com.ossobo.winterfx.scanner.enums.ScopeType;
import com.ossobo.winterfx.scanner.models.BeanDefinition;
import com.ossobo.winterfx.scanner.models.InjectionPoint;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;

import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Scanner de beans do WinterFX.
 *
 * <p>Recebe um ScanResult compartilhado do ScannerEngine.</p>
 *
 * <p>Responsabilidade: identificar classes anotadas com {@code @Controller},
 * {@code @Service}, {@code @Repository}, {@code @Component} e
 * {@code @Configuration} + {@code @Bean}, extraindo seus metadados
 * e registrando no {@link BeanRegistry}.</p>
 */
public final class BeanAnnotationScanner {

    private static final Set<Class<? extends Annotation>> BEAN_ANNOTATIONS =
            Set.of(Component.class, Service.class, Repository.class, Controller.class);

    private final ScanResult scanResult;
    private final BeanMetadataExtractor metadataExtractor = new BeanMetadataExtractor(new ReflectionScanner());

    public BeanAnnotationScanner(ScanResult scanResult) {
        this.scanResult = scanResult;
    }

    public int scanAndRegister(BeanRegistry registry) {
        int count = 0;
        count += scanComponents(registry);
        count += scanConfigurations(registry);
        return count;
    }

    private int scanComponents(BeanRegistry registry) {
        Set<String> classNames = new LinkedHashSet<>();
        for (Class<? extends Annotation> ann : BEAN_ANNOTATIONS) {
            classNames.addAll(scanResult.getClassesWithAnnotation(ann).getNames());
        }

        int count = 0;
        for (String className : classNames) {
            Class<?> type = loadClass(className);
            if (type == null || type.isInterface() || type.isAnnotation() || type.isEnum()) {
                continue;
            }

            registerComponent(type, registry);
            count++;
        }
        return count;
    }

    private int scanConfigurations(BeanRegistry registry) {
        int count = 0;
        for (String className : scanResult.getClassesWithAnnotation(Configuration.class).getNames()) {
            Class<?> type = loadClass(className);
            if (type == null || type.isInterface() || type.isAnnotation() || type.isEnum()) {
                continue;
            }

            registerConfiguration(type, registry);
            count++;
        }
        return count;
    }

    /**
     * Registra um componente (Controller, Service, Repository, Component).
     *
     * <p>Extrai todos os metadados necessários e registra no BeanRegistry.</p>
     *
     * @param type tipo do componente
     * @param registry registro de beans
     */
    private void registerComponent(Class<?> type, BeanRegistry registry) {
        String beanName = getBeanName(type);
        ScopeType scopeType = determineScope(type);
        List<InjectionPoint> dependencies = metadataExtractor.extractInjectionPoints(type);

        // ✅ VALIDA @PostConstruct ANTES de registrar
        Method postConstruct = metadataExtractor.extractPostConstruct(type);
        validatePostConstruct(type, postConstruct);

        Method preDestroy = metadataExtractor.extractPreDestroy(type);

        boolean primary = type.isAnnotationPresent(Primary.class);
        String qualifier = extractQualifier(type);
        Map<String, String> values = metadataExtractor.extractValues(type);

        registry.registerDefinition(new BeanDefinition(
                beanName, type, scopeType, dependencies, postConstruct, preDestroy,
                primary, qualifier, values));
    }

    /**
     * Registra uma classe de configuração com seus beans factory.
     *
     * @param configClass classe de configuração
     * @param registry registro de beans
     */
    private void registerConfiguration(Class<?> configClass, BeanRegistry registry) {
        String beanName = getConfigBeanName(configClass);
        ScopeType scopeType = determineScope(configClass);
        List<InjectionPoint> dependencies = metadataExtractor.extractInjectionPoints(configClass);

        // ✅ VALIDA @PostConstruct ANTES de registrar
        Method postConstruct = metadataExtractor.extractPostConstruct(configClass);
        validatePostConstruct(configClass, postConstruct);

        Method preDestroy = metadataExtractor.extractPreDestroy(configClass);

        boolean primary = configClass.isAnnotationPresent(Primary.class);
        String qualifier = extractQualifier(configClass);
        Map<String, String> values = metadataExtractor.extractValues(configClass);

        registry.registerDefinition(new BeanDefinition(
                beanName, configClass, scopeType, dependencies, postConstruct, preDestroy,
                primary, qualifier, values));

        // Processa métodos @Bean
        for (Method method : configClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Bean.class)) {
                registerFactoryBean(configClass, method, registry);
            }
        }
    }

    /**
     * Registra um bean criado por factory method (@Configuration + @Bean).
     *
     * @param factoryClass classe que contém o factory method
     * @param factoryMethod método factory
     * @param registry registro de beans
     */
    private void registerFactoryBean(Class<?> factoryClass, Method factoryMethod, BeanRegistry registry) {
        Bean bean = factoryMethod.getAnnotation(Bean.class);
        String beanName = (bean != null && bean.name() != null && !bean.name().isBlank())
                ? bean.name() : factoryMethod.getName();

        Class<?> beanType = factoryMethod.getReturnType();
        ScopeType scopeType = determineScope(factoryMethod);
        List<InjectionPoint> dependencies = metadataExtractor.extractInjectionPoints(beanType);

        // ✅ VALIDA @PostConstruct do beanType
        Method postConstruct = metadataExtractor.extractPostConstruct(beanType);
        validatePostConstruct(beanType, postConstruct);

        Method preDestroy = metadataExtractor.extractPreDestroy(beanType);

        boolean primary = factoryMethod.isAnnotationPresent(Primary.class);
        String qualifier = extractQualifier(factoryMethod);
        Map<String, String> values = metadataExtractor.extractValues(beanType);

        registry.registerDefinition(new BeanDefinition(
                beanName, beanType, scopeType, factoryClass, factoryMethod,
                dependencies, postConstruct, preDestroy, primary, qualifier, values));
    }

    /**
     * Valida se o método @PostConstruct atende aos requisitos.
     *
     * <p>Requisitos:</p>
     * <ul>
     *   <li>Método deve retornar {@code void}</li>
     *   <li>Método não deve ter parâmetros</li>
     * </ul>
     *
     * @param type classe que contém o método
     * @param method método @PostConstruct (pode ser null)
     * @throws IllegalStateException se o método não atender aos requisitos
     */
    private void validatePostConstruct(Class<?> type, Method method) {
        if (method == null) {
            return;
        }

        // ✅ Apenas valida, não executa
        if (method.getReturnType() != void.class) {
            throw new IllegalStateException(
                    "@PostConstruct em " + type.getName() + "." + method.getName() +
                            " deve retornar void"
            );
        }

        if (method.getParameterCount() > 0) {
            throw new IllegalStateException(
                    "@PostConstruct em " + type.getName() + "." + method.getName() +
                            " não pode ter parâmetros"
            );
        }
    }

    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================

    private String getBeanName(Class<?> type) {
        for (Class<? extends Annotation> ann : BEAN_ANNOTATIONS) {
            if (type.isAnnotationPresent(ann)) {
                try {
                    Method valueMethod = ann.getMethod("value");
                    String value = (String) valueMethod.invoke(type.getAnnotation(ann));
                    if (value != null && !value.isBlank()) {
                        return value;
                    }
                } catch (Exception ignored) {
                    // Ignora e continua
                }
            }
        }
        String simple = type.getSimpleName();
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }

    private String getConfigBeanName(Class<?> configClass) {
        Configuration config = configClass.getAnnotation(Configuration.class);
        return (config == null || config.value().isBlank())
                ? getDefaultBeanName(configClass)
                : config.value();
    }

    private String getDefaultBeanName(Class<?> type) {
        String simple = type.getSimpleName();
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }

    private ScopeType determineScope(Class<?> type) {
        Scope scope = type.getAnnotation(Scope.class);
        return scope != null ? scope.value() : ScopeType.SINGLETON;
    }

    private ScopeType determineScope(Method method) {
        Scope scope = method.getAnnotation(Scope.class);
        return scope != null ? scope.value() : ScopeType.SINGLETON;
    }

    private String extractQualifier(Class<?> type) {
        Qualifier q = type.getAnnotation(Qualifier.class);
        return q != null ? q.value() : null;
    }

    private String extractQualifier(Method method) {
        Qualifier q = method.getAnnotation(Qualifier.class);
        return q != null ? q.value() : null;
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (Throwable e) {
            return null;
        }
    }
}