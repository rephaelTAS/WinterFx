package com.ossobo.winterfx.scanner;

import com.ossobo.winterfx.anotations.*;
import com.ossobo.winterfx.anotations.Configuration;
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
 * NOTA DE DESIGN: classes com @PropertySource SEM estereótipo são registradas
 * como beans (via registerComponent) apenas para chegarem ao
 * PropertySourceProcessor — o mesmo comportamento do Spring com @Configuration.
 * Consequência: o container INSTANCIA essas classes; garanta construtor sem args
 * ou dependências resolúveis.
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
        count += scanPropertySources(registry);
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
     */
    private void registerComponent(Class<?> type, BeanRegistry registry) {
        String beanName = getBeanName(type);
        ScopeType scopeType = determineScope(type);
        List<InjectionPoint> dependencies = metadataExtractor.extractInjectionPoints(type);

        // extractPostConstruct já valida e lança exceção se inválido
        Method postConstruct = metadataExtractor.extractPostConstruct(type);
        Method preDestroy = metadataExtractor.extractPreDestroy(type);

        boolean primary = type.isAnnotationPresent(Primary.class);
        String qualifier = extractQualifier(type);
        Map<String, String> values = metadataExtractor.extractValues(type);

        registry.registerDefinition(BeanDefinition.component(
                beanName, type, scopeType, dependencies, postConstruct, preDestroy,
                primary, qualifier, values));
    }

    /**
     * Registra uma classe de configuração com seus beans factory.
     */
    private void registerConfiguration(Class<?> configClass, BeanRegistry registry) {
        String beanName = getConfigBeanName(configClass);
        ScopeType scopeType = determineScope(configClass);
        List<InjectionPoint> dependencies = metadataExtractor.extractInjectionPoints(configClass);

        // extractPostConstruct já valida e lança exceção se inválido
        Method postConstruct = metadataExtractor.extractPostConstruct(configClass);
        Method preDestroy = metadataExtractor.extractPreDestroy(configClass);

        boolean primary = configClass.isAnnotationPresent(Primary.class);
        String qualifier = extractQualifier(configClass);
        Map<String, String> values = metadataExtractor.extractValues(configClass);

        registry.registerDefinition(BeanDefinition.component(
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
     */
    private void registerFactoryBean(Class<?> factoryClass, Method factoryMethod, BeanRegistry registry) {
        var bean = factoryMethod.getAnnotation(Bean.class);
        String beanName = (bean != null && bean.name() != null && !bean.name().isBlank())
                ? bean.name() : factoryMethod.getName();

        Class<?> beanType = factoryMethod.getReturnType();
        ScopeType scopeType = determineScope(factoryMethod);

        // Dependências de @Bean vêm dos PARÂMETROS do método, não da classe de retorno
        List<InjectionPoint> dependencies = metadataExtractor.extractMethodParameters(factoryMethod);

        // extractPostConstruct já valida e lança exceção se inválido
        Method postConstruct = metadataExtractor.extractPostConstruct(beanType);
        Method preDestroy = metadataExtractor.extractPreDestroy(beanType);

        boolean primary = factoryMethod.isAnnotationPresent(Primary.class);
        String qualifier = extractQualifier(factoryMethod);

        // @Value não se aplica a beans criados por fábrica
        Map<String, String> values = Collections.emptyMap();

        registry.registerDefinition(BeanDefinition.factory(
                beanName, beanType, scopeType, factoryClass, factoryMethod,
                dependencies, postConstruct, preDestroy, primary, qualifier, values));
    }

    /**
     * Registra classes que declaram @PropertySource mesmo SEM @Configuration.
     *
     * CONTRATO: o PropertySourceProcessor só enxerga classes presentes no
     * BeanRegistry. Sem este passo, @PropertySource "solto" (sem estereótipo)
     * era silenciosamente ignorado — o pior tipo de falha de configuração.
     * A varredura usa o MESMO ScanResult (classgraph) dos demais: zero custo extra.
     */
    private int scanPropertySources(BeanRegistry registry) {
        int count = 0;
        for (String className : scanResult.getClassesWithAnnotation(PropertySource.class).getNames()) {
            Class<?> type = loadClass(className);
            if (type == null || type.isInterface() || type.isAnnotation() || type.isEnum()) {
                continue;
            }
            // Idempotência por TIPO EXATO: classe com @Configuration + @PropertySource
            // já foi registrada acima e não duplica. isRegisteredExact evita o falso
            // positivo de subclasses (isRegistered retornaria true e suprimiria o registro).
            if (!registry.isRegisteredExact(type)) {
                registerComponent(type, registry);
                count++;
            }
        }
        return count;
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
        return getDefaultBeanName(type);
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