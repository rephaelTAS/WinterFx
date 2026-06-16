package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.anotations.Value;
import com.ossobo.winterfx.di.configuration.ConfigurationManager;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;

import java.lang.reflect.Field;
import java.util.List;

public class ValueInjector implements DependencyInjector {

    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final ConfigurationManager configurationManager;

    public ValueInjector(ReflectionCache reflectionCache, ReflectionProcessor reflectionProcessor, ConfigurationManager configurationManager) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.configurationManager = configurationManager;
    }

    public ReflectionCache getReflectionCache() {
        return reflectionCache;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public ReflectionProcessor getReflectionProcessor() {
        return reflectionProcessor;
    }

    /**
     * Injeta valores de configuração em campos anotados com @Value.
     * Suporta placeholders ${...} e valores padrão ${key:default}.
     */

    @Override
    public void inject(Object instance, Class<?> type) {
        List<Field> fields = reflectionCache.getInjectableFields(type);

        for (Field field : fields) {
            if (field.isAnnotationPresent(Value.class)) {
                Value valueAnnotation = field.getAnnotation(Value.class);
                String expression = valueAnnotation.value();
                Object resolvedValue = resolveValue(expression, field.getType());
                reflectionProcessor.injectField(instance, field, resolvedValue);
            }
        }
    }



    /**
     * Resolve uma expressão @Value.
     * Ex: "${app.name}" → "MyApp"
     * Ex: "${app.port:8080}" → "8080" (se não definido)
     */
    private Object resolveValue(String expression, Class<?> targetType) {
        String resolved = configurationManager.resolvePlaceholder(expression);

        if (resolved == null) return null;

        // Conversão de tipos básicos
        if (targetType == String.class) return resolved;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(resolved);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(resolved);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(resolved);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(resolved);

        return resolved;
    }
}