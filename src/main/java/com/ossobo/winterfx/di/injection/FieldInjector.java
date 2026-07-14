package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.anotations.Qualifier;
import com.ossobo.winterfx.anotations.Value;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Injetor de dependências baseado em campos anotados com {@code @Inject}.
 *
 * <p>Esta classe é responsável por resolver e injetar dependências em campos de um bean,
 * suportando resolução por tipo, por nome (utilizando {@code @Qualifier}) e injeção
 * de todas as implementações de uma interface em coleções ({@code List} ou {@code Set}).</p>
 *
 * <p>Este injetor é estritamente desacoplado de módulos de alto nível como o {@code StageManager}.
 * A resolução de controllers FXML ativos é delegada a injetores externos registrados
 * posteriormente no {@link InjectionManager}.</p>
 *
 * <p><b>Ordem de resolução para um campo:</b></p>
 * <ol>
 *   <li>Se o campo for do tipo {@code Collection} ou {@code Set}, resolve todos os beans do tipo genérico.</li>
 *   <li>Se o campo possuir a anotação {@code @Qualifier}, busca o bean pelo nome qualificador e tipo.</li>
 *   <li>Caso contrário, realiza a busca exclusivamente pelo tipo do campo.</li>
 * </ol>
 *
 * @version 2.0
 */
public class FieldInjector implements DependencyInjector {

    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final DependencyResolver dependencyResolver;

    /**
     * Constrói um novo injetor de campos.
     *
     * @param reflectionCache    Cache de metadados de reflexão para otimização de busca de campos.
     * @param reflectionProcessor Utilitário para realizar a injeção segura via reflexão.
     * @param dependencyResolver  Resolvedor de dependências do contêiner de DI.
     */
    public FieldInjector(ReflectionCache reflectionCache,
                         ReflectionProcessor reflectionProcessor,
                         DependencyResolver dependencyResolver) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.dependencyResolver = dependencyResolver;
    }

    /**
     * Analisa e injeta dependências em todos os campos anotados com {@code @Inject}
     * da instância fornecida.
     *
     * <p>Campos anotados com {@code @Value} são ignorados por este injetor,
     * sendo de responsabilidade do {@link ValueInjector}.</p>
     *
     * @param instance A instância do bean a ser processada.
     * @param type     O tipo (Classe) do bean sendo processado.
     */
    @Override
    public void inject(Object instance, Class<?> type) {
        List<Field> fields = reflectionCache.getInjectableFields(type);

        for (Field field : fields) {
            if (field.isAnnotationPresent(Value.class)) {
                continue;
            }

            Object dependency = resolveFieldDependency(field);
            reflectionProcessor.injectField(instance, field, dependency);
        }
    }

    // ============================================================
    // RESOLUÇÃO DE DEPENDÊNCIA DO CAMPO
    // ============================================================

    /**
     * Determina a estratégia de resolução com base nas características do campo
     * (coleção, qualificador ou tipo simples) e retorna a dependência correspondente.
     *
     * @param field O campo a ser resolvido.
     * @return A instância da dependência resolveda.
     */
    private Object resolveFieldDependency(Field field) {
        Class<?> fieldType = field.getType();
        java.lang.reflect.Type genericType = field.getGenericType();

        if (Collection.class.isAssignableFrom(fieldType)) {
            return resolveCollection(genericType);
        }

        String qualifier = getQualifier(field);
        if (qualifier != null) {
            return dependencyResolver.getBean(fieldType, qualifier);
        }

        return dependencyResolver.getBean(fieldType);
    }

    // ============================================================
    // RESOLUÇÃO DE COLEÇÕES
    // ============================================================

    /**
     * Resolve uma injeção de coleção, buscando todas as implementações
     * do tipo genérico especificado e retornando como {@code List} ou {@code Set}.
     *
     * @param collectionType O tipo genérico da coleção (ex: {@code List<MeuBean>}).
     * @return Uma coleção contendo todas as instâncias encontradas.
     * @throws IllegalArgumentException se a coleção não for parametrizada ou o tipo não for suportado.
     */
    @SuppressWarnings("unchecked")
    private Object resolveCollection(java.lang.reflect.Type collectionType) {
        if (!(collectionType instanceof java.lang.reflect.ParameterizedType pt)) {
            throw new IllegalArgumentException(
                    "Coleção deve ser genérica: " + collectionType);
        }

        Class<?> elementType = (Class<?>) pt.getActualTypeArguments()[0];
        Class<?> rawType = (Class<?>) pt.getRawType();

        List<?> implementations = dependencyResolver.getAllBeansOfType(elementType);

        if (List.class.isAssignableFrom(rawType)) {
            return implementations;
        }
        if (Set.class.isAssignableFrom(rawType)) {
            return new java.util.HashSet<>(implementations);
        }

        throw new IllegalArgumentException(
                "Tipo de coleção não suportado: " + rawType.getSimpleName() +
                        ". Use List ou Set.");
    }

    // ============================================================
    // EXTRAÇÃO DE @QUALIFIER
    // ============================================================

    /**
     * Extrai o valor da anotação {@code @Qualifier} presente no campo.
     *
     * @param field O campo a ser verificado.
     * @return O nome do qualificador, ou {@code null} se ausente ou vazio.
     */
    private String getQualifier(Field field) {
        if (field.isAnnotationPresent(Qualifier.class)) {
            String value = field.getAnnotation(Qualifier.class).value();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return null;
    }
}