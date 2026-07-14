package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.anotations.Qualifier;
import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.di.resolver.DependencyResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Injetor de dependências baseado em métodos anotados com {@code @Inject}.
 *
 * <p>Esta classe identifica métodos marcados para injeção, resolve todas as
 * dependências requeridas em seus parâmetros e invoca o método com os argumentos
 * corretamente preenchidos.</p>
 *
 * <p>Segue a mesma lógica de resolução do {@link FieldInjector}, suportando
 * injeção por tipo, por {@code @Qualifier} e injeção de múltiplas implementações
 * via coleções ({@code List} ou {@code Set}).</p>
 */
public class MethodInjector implements DependencyInjector {

    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final DependencyResolver dependencyResolver;

    /**
     * Constrói um novo injetor de métodos.
     *
     * @param reflectionCache    Cache de metadados de reflexão para otimização de busca de métodos.
     * @param reflectionProcessor Utilitário para realizar a invocação segura via reflexão.
     * @param dependencyResolver  Resolvedor de dependências do contêiner de DI.
     */
    public MethodInjector(ReflectionCache reflectionCache,
                          ReflectionProcessor reflectionProcessor,
                          DependencyResolver dependencyResolver) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.dependencyResolver = dependencyResolver;
    }

    /**
     * Identifica os métodos anotados com {@code @Inject} no tipo fornecido,
     * resolve seus parâmetros e executa a invocação na instância alvo.
     *
     * @param instance A instância do bean a ser processada.
     * @param type     O tipo (Classe) do bean sendo processado.
     */
    @Override
    public void inject(Object instance, Class<?> type) {
        List<Method> methods = reflectionCache.getInjectableMethods(type);

        for (Method method : methods) {
            Object[] args = resolveMethodParameters(method);
            reflectionProcessor.invokeMethod(instance, method, args);
        }
    }

    /**
     * Itera sobre os parâmetros de um método e constrói um array de argumentos resolvedos.
     *
     * @param method O método cujos parâmetros serão resolvidos.
     * @return Um array de objetos contendo as dependências na ordem exigida pelo método.
     */
    private Object[] resolveMethodParameters(Method method) {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            args[i] = resolveParameter(params[i]);
        }

        return args;
    }

    /**
     * Determina a estratégia de resolução para um parâmetro individual de método.
     *
     * @param param O parâmetro a ser resolvido.
     * @return A instância da dependência resolveda.
     */
    private Object resolveParameter(Parameter param) {
        Class<?> paramType = param.getType();
        java.lang.reflect.Type genericType = param.getParameterizedType();

        if (Collection.class.isAssignableFrom(paramType)) {
            return resolveCollection(genericType);
        }

        String qualifier = getQualifier(param);
        if (qualifier != null) {
            return dependencyResolver.getBean(paramType, qualifier);
        }

        return dependencyResolver.getBean(paramType);
    }

    /**
     * Resolve uma injeção de coleção em um parâmetro de método, buscando todas
     * as implementações do tipo genérico especificado.
     *
     * @param collectionType O tipo genérico da coleção.
     * @return Uma coleção contendo todas as instâncias encontradas.
     * @throws IllegalArgumentException se a coleção não for parametrizada ou o tipo não for suportado.
     */
    @SuppressWarnings("unchecked")
    private Object resolveCollection(java.lang.reflect.Type collectionType) {
        if (!(collectionType instanceof java.lang.reflect.ParameterizedType pt)) {
            throw new IllegalArgumentException("Coleção deve ser genérica: " + collectionType);
        }

        Class<?> elementType = (Class<?>) pt.getActualTypeArguments()[0];
        Class<?> rawType = (Class<?>) pt.getRawType();

        List<?> implementations = dependencyResolver.getAllBeansOfType(elementType);

        if (List.class.isAssignableFrom(rawType)) {
            return implementations;
        } else if (Set.class.isAssignableFrom(rawType)) {
            return new java.util.HashSet<>(implementations);
        }

        throw new IllegalArgumentException("Tipo de coleção não suportado: " + rawType);
    }

    /**
     * Extrai o valor da anotação {@code @Qualifier} presente no parâmetro.
     *
     * @param param O parâmetro a ser verificado.
     * @return O nome do qualificador, ou {@code null} se ausente ou vazio.
     */
    private String getQualifier(Parameter param) {
        if (param.isAnnotationPresent(Qualifier.class)) {
            String value = param.getAnnotation(Qualifier.class).value();
            if (!value.isEmpty()) return value;
        }
        return null;
    }
}