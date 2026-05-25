package com.ossobo.winterfx.di.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;

/**
 * ReflectionProcessor v2.1
 *
 * Utilitário de baixo nível para manipulação segura de reflection.
 *
 * Responsabilidades:
 * - Injetar valores em campos (privados ou não)
 * - Invocar métodos (privados ou não)
 * - Instanciar classes via construtor (privado ou não)
 * - Garantir restauração do estado de acessibilidade
 * - Suporte a processamento em lote de anotações ← NOVO!
 *
 * Usado por InjectionManager, InstanceCreator, StageManager e ImageManager.
 * Sem estado interno — thread-safe.
 *
 * @since 2.1
 */
public final class ReflectionProcessor {

    private static final Logger LOGGER = Logger.getLogger(ReflectionProcessor.class.getName());

    public ReflectionProcessor() {}

    // =============================================
    // INJEÇÃO DE CAMPO
    // =============================================

    /**
     * Injeta um valor num campo específico de uma instância.
     * Trata campos privados e restaura o estado de acessibilidade.
     *
     * @param instance instância alvo
     * @param field    campo a injetar
     * @param value    valor a atribuir
     */
    public void injectField(Object instance, Field field, Object value) {
        boolean wasAccessible = field.canAccess(instance);

        try {
            if (!wasAccessible) {
                field.setAccessible(true);
            }
            field.set(instance, value);

            LOGGER.log(Level.FINE, "✅ Campo injetado: {0}.{1} = {2}",
                    new Object[]{instance.getClass().getSimpleName(),
                            field.getName(),
                            value != null ? value.getClass().getSimpleName() : "null"});
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    "Erro ao injetar campo '" + field.getName() +
                            "' em '" + instance.getClass().getName() + "': " + e.getMessage(), e);
        } finally {
            if (!wasAccessible) {
                field.setAccessible(false);
            }
        }
    }

    /**
     * Injeta múltiplos campos de uma vez (processamento em lote).
     *
     * @param instance   instância alvo
     * @param fieldValues mapa de campo → valor
     */
    public void injectFields(Object instance, Map<Field, Object> fieldValues) {
        for (Map.Entry<Field, Object> entry : fieldValues.entrySet()) {
            injectField(instance, entry.getKey(), entry.getValue());
        }
    }

    // =============================================
    // INVOCAÇÃO DE MÉTODO
    // =============================================

    /**
     * Invoca um método numa instância com os argumentos fornecidos.
     * Trata métodos privados e restaura o estado de acessibilidade.
     *
     * @param instance instância alvo
     * @param method   método a invocar
     * @param args     argumentos do método
     * @return valor de retorno do método, ou null se void
     */
    public Object invokeMethod(Object instance, Method method, Object... args) {
        boolean wasAccessible = method.canAccess(instance);

        try {
            if (!wasAccessible) {
                method.setAccessible(true);
            }
            return method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Erro ao invocar método '" + method.getName() +
                            "' em '" + instance.getClass().getName() + "': " + e.getMessage(), e);
        } finally {
            if (!wasAccessible) {
                method.setAccessible(false);
            }
        }
    }

    /**
     * Invoca múltiplos métodos em sequência.
     *
     * @param instance     instância alvo
     * @param methods      lista de métodos a invocar
     * @param argsProvider fornecedor de argumentos por método
     */
    public void invokeMethods(Object instance, List<Method> methods,
                              java.util.function.Function<Method, Object[]> argsProvider) {
        for (Method method : methods) {
            Object[] args = argsProvider.apply(method);
            invokeMethod(instance, method, args);
        }
    }

    // =============================================
    // INSTANCIAÇÃO
    // =============================================

    /**
     * Cria uma nova instância via construtor.
     * Trata construtores privados e restaura o estado de acessibilidade.
     *
     * @param constructor construtor a usar
     * @param args        argumentos do construtor
     * @param <T>         tipo da instância
     * @return nova instância
     */
    @SuppressWarnings("unchecked")
    public <T> T instantiate(Constructor<?> constructor, Object... args) {
        boolean wasAccessible = constructor.canAccess(null);

        try {
            if (!wasAccessible) {
                constructor.setAccessible(true);
            }
            T instance = (T) constructor.newInstance(args);
            LOGGER.log(Level.FINE, "✅ Instância criada: {0}",
                    instance.getClass().getSimpleName());
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Erro ao instanciar '" + constructor.getDeclaringClass().getName() +
                            "': " + e.getMessage(), e);
        } finally {
            if (!wasAccessible) {
                constructor.setAccessible(false);
            }
        }
    }

    // =============================================
    // LEITURA DE CAMPO
    // =============================================

    /**
     * Lê o valor de um campo de uma instância.
     *
     * @param instance instância alvo
     * @param field    campo a ler
     * @return valor do campo
     */
    public Object readField(Object instance, Field field) {
        boolean wasAccessible = field.canAccess(instance);

        try {
            if (!wasAccessible) {
                field.setAccessible(true);
            }
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    "Erro ao ler campo '" + field.getName() +
                            "' de '" + instance.getClass().getName() + "': " + e.getMessage(), e);
        } finally {
            if (!wasAccessible) {
                field.setAccessible(false);
            }
        }
    }

    // =============================================
    // PROCESSAMENTO DE ANOTAÇÕES (NOVO!)
    // =============================================

    /**
     * Processa todos os campos anotados com @InjectView, @GetController, @InjectImage
     * usando os respectivos managers.
     *
     * @param instance     instância a processar
     * @param stageManager gerenciador de views (pode ser null)
     * @param imageManager gerenciador de imagens (pode ser null)
     */
    public void processResourceAnnotations(Object instance,
                                           Object stageManager,
                                           Object imageManager) {
        if (instance == null) return;

        Class<?> type = instance.getClass();

        // Processa @InjectView e @GetController via StageManager
        if (stageManager != null) {
            try {
                Method processAnnotations = stageManager.getClass()
                        .getMethod("processAnnotations", Object.class);
                processAnnotations.invoke(stageManager, instance);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Erro ao processar anotações de view: {0}",
                        e.getMessage());
            }
        }

        // Processa @InjectImage via ImageManager
        if (imageManager != null) {
            try {
                Method processAnnotations = imageManager.getClass()
                        .getMethod("processAnnotations", Object.class);
                processAnnotations.invoke(imageManager, instance);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Erro ao processar anotações de imagem: {0}",
                        e.getMessage());
            }
        }
    }

    // =============================================
    // VERIFICAÇÃO DE ANOTAÇÕES
    // =============================================

    /**
     * Verifica se um campo tem uma anotação específica.
     */
    public boolean hasAnnotation(Field field, Class<? extends java.lang.annotation.Annotation> annotation) {
        return field.isAnnotationPresent(annotation);
    }

    /**
     * Verifica se um método tem uma anotação específica.
     */
    public boolean hasAnnotation(Method method, Class<? extends java.lang.annotation.Annotation> annotation) {
        return method.isAnnotationPresent(annotation);
    }

    /**
     * Obtém o valor de uma anotação em um campo.
     */
    @SuppressWarnings("unchecked")
    public <T> T getFieldAnnotationValue(Field field, String methodName) {
        for (java.lang.annotation.Annotation ann : field.getAnnotations()) {
            try {
                Method valueMethod = ann.annotationType().getMethod(methodName);
                return (T) valueMethod.invoke(ann);
            } catch (Exception ignored) {}
        }
        return null;
    }
}