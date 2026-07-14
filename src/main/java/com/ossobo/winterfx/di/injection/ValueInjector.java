package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.anotations.Value;
import com.ossobo.winterfx.di.configuration.ConfigurationManager;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Injetor de valores de configuração baseado na anotação {@code @Value}.
 *
 * <p>Esta classe é responsável por resolver expressões de configuração e injetá-las
 * nos campos anotados de um bean, com suporte a conversão automática de tipos.</p>
 *
 * <p><b>Formatos de expressão suportados:</b></p>
 * <ul>
 *   <li>{@code ${chave}} - Busca o valor associado à chave no gerenciador de configuração.</li>
 *   <li>{@code ${chave:valorPadrao}} - Utiliza o valor padrão caso a chave não exista.</li>
 *   <li>{@code "literal"} - Injeta a string literal, sem resolução.</li>
 * </ul>
 *
 * <p><b>Conversão automática de tipos suportada:</b> {@code String}, {@code int}/{@code Integer},
 * {@code long}/{@code Long}, {@code boolean}/{@code Boolean} e {@code double}/{@code Double}.</p>
 *
 * @version 2.0
 */
public class ValueInjector implements DependencyInjector {

    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final ConfigurationManager configurationManager;

    /**
     * Constrói um novo injetor de valores.
     *
     * @param reflectionCache       Cache de metadados de reflexão para busca de campos.
     * @param reflectionProcessor    Utilitário para realizar a injeção segura via reflexão.
     * @param configurationManager  Gerenciador de configurações utilizado para resolver os placeholders.
     */
    public ValueInjector(ReflectionCache reflectionCache,
                         ReflectionProcessor reflectionProcessor,
                         ConfigurationManager configurationManager) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.configurationManager = configurationManager;
    }

    /**
     * Identifica campos anotados com {@code @Value}, resolve suas expressões
     * correspondentes e injeta os valores convertidos na instância alvo.
     *
     * <p>Caso o {@link ConfigurationManager} não tenha sido inicializado,
     * a execução deste método é abortada silenciosamente.</p>
     *
     * @param instance A instância do bean a ser processada.
     * @param type     O tipo (Classe) do bean sendo processado.
     */
    @Override
    public void inject(Object instance, Class<?> type) {
        if (configurationManager == null) {
            return;
        }

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
     * Resolve uma expressão oriunda da anotação {@code @Value} e a converte
     * para o tipo de destino exigido pelo campo.
     *
     * <p>A resolução suporta placeholders aninhados de forma recursiva.
     * Se a resolução falhar, a expressão literal original é retornada.
     * Se a conversão de tipo falhar, são retornados valores padrão seguros
     * (como {@code 0}, {@code 0L}, {@code 0.0} ou {@code false}).</p>
     *
     * @param expression  A expressão definida na anotação (ex: {@code "${app.port:8080}"}).
     * @param targetType  O tipo de dado do campo que receberá o valor.
     * @return O valor resolvido e convertido para o tipo de destino.
     */
    private Object resolveValue(String expression, Class<?> targetType) {
        if (expression == null) {
            return null;
        }

        String resolved = configurationManager.resolveRecursive(expression);

        if (resolved == null) {
            resolved = expression;
        }

        if (targetType == String.class) {
            return resolved;
        }
        if (targetType == int.class || targetType == Integer.class) {
            try {
                return Integer.parseInt(resolved);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if (targetType == long.class || targetType == Long.class) {
            try {
                return Long.parseLong(resolved);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(resolved);
        }
        if (targetType == double.class || targetType == Double.class) {
            try {
                return Double.parseDouble(resolved);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        return resolved;
    }
}