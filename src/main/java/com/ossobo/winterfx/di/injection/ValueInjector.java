package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.anotations.Value;
import com.ossobo.winterfx.di.configuration.ConfigurationManager;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * Injetor de valores de configuração baseado na anotação {@code @Value}.
 *
 * <p><b>Formatos de expressão suportados:</b></p>
 * <ul>
 *   <li>{@code ${chave}} - Resolve a chave no ConfigurationManager.</li>
 *   <li>{@code ${chave:valorPadrao}} - Usa o default se a chave não existir.</li>
 * </ul>
 *
 * <p><b>CONTRATO FAIL-FAST:</b> placeholder não resolvido sem default, valor
 * vazio ou conversão impossível = {@link IllegalStateException} com contexto
 * completo (classe.campo). Configuração errada quebra o BOOT — nunca produz
 * 0/false/"" silenciosamente.</p>
 *
 * @version 3.0
 */
public class ValueInjector implements DependencyInjector {

    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final ConfigurationManager configurationManager;

    public ValueInjector(ReflectionCache reflectionCache,
                         ReflectionProcessor reflectionProcessor,
                         ConfigurationManager configurationManager) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.configurationManager = configurationManager;
    }

    /**
     * Injeta valores em todos os campos anotados com @Value.
     *
     * <p>Falha imediatamente se o ConfigurationManager não foi fornecido —
     * seguir silenciosamente deixaria todos os @Value como null.</p>
     */
    @Override
    public void inject(Object instance, Class<?> type) {
        Objects.requireNonNull(configurationManager,
                "ConfigurationManager não configurado no ValueInjector — " +
                        "verifique se setConfigurationManager() roda ANTES de initCoreInjectors()");

        List<Field> fields = reflectionCache.getInjectableFields(type);

        for (Field field : fields) {
            if (field.isAnnotationPresent(Value.class)) {
                Value valueAnnotation = field.getAnnotation(Value.class);
                // Field passado adiante: permite mensagens de erro com
                // contexto Classe.campo — essencial para diagnóstico
                Object resolvedValue = resolveValue(valueAnnotation.value(), field);
                reflectionProcessor.injectField(instance, field, resolvedValue);
            }
        }
    }

    /**
     * Resolve a expressão do @Value para o campo, com validação estrita.
     */
    private Object resolveValue(String expression, Field field) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalStateException("@Value sem expressão no campo " + field);
        }

        String resolved = configurationManager.resolveRecursive(expression);
        String contexto = field.getDeclaringClass().getSimpleName() + "." + field.getName();

        // resolveRecursive retorna "" para ${chave} inexistente sem default.
        // Aceitar isso seria injetar vazio silenciosamente.
        if (resolved == null || resolved.isBlank()) {
            throw new IllegalStateException(
                    "Placeholder não resolvido: '" + expression + "' (campo " + contexto +
                            "). Verifique o application.properties, o @PropertySource, " +
                            "ou declare um default: ${chave:valor}");
        }

        return convertValue(resolved, field.getType(), expression, contexto);
    }

    /** Conversão de tipos única. Falha reportada com chave, valor, tipo e campo. */
    private Object convertValue(String resolved, Class<?> targetType,
                                String expression, String contexto) {
        try {
            if (targetType == String.class)   return resolved;
            if (targetType == int.class     || targetType == Integer.class) return Integer.parseInt(resolved.trim());
            if (targetType == long.class    || targetType == Long.class)    return Long.parseLong(resolved.trim());
            if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(resolved.trim());
            if (targetType == double.class  || targetType == Double.class)  return Double.parseDouble(resolved.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Valor '" + resolved + "' da expressão '" + expression +
                            "' não é conversível para " + targetType.getSimpleName() +
                            " (campo " + contexto + ")", e);
        }

        throw new IllegalStateException(
                "Tipo não suportado por @Value: " + targetType.getName() +
                        " (campo " + contexto + "). Suportados: String, int, long, boolean, double + wrappers");
    }
}