package com.ossobo.winterfx.di.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.List;

/**
 * Utilitário de baixo nível para execução segura de operações de reflexão.
 *
 * <p>Esta classe encapsula as chamadas à API de reflexão do Java, garantindo que
 * membros privados possam ser acessados e restaurando corretamente o estado original
 * de acessibilidade ({@code accessible}) após a execução das operações, prevenindo
 * vazamentos de segurança em ambientes com gerenciadores de segurança ativados.</p>
 *
 * <p>Principais operações suportadas:</p>
 * <ul>
 *   <li>Injeção de valores em campos.</li>
 *   <li>Leitura de valores de campos.</li>
 *   <li>Invocação de métodos.</li>
 *   <li>Instanciação via construtores.</li>
 * </ul>
 *
 * <p>Por não manter estado interno, esta classe é inerentemente thread-safe.</p>
 *
 * @since 3.0
 */
public final class ReflectionProcessor {

    public ReflectionProcessor() {}

    // =============================================
    // INJEÇÃO DE CAMPO
    // =============================================

    /**
     * Injeta um valor em um campo específico de uma instância, contornando
     * o modificador de acesso ({@code private}, {@code protected}), se necessário.
     *
     * @param instance A instância alvo da injeção.
     * @param field    O campo a ser modificado.
     * @param value    O valor a ser atribuído ao campo.
     * @throws RuntimeException Se a injeção falhar devido a restrições de segurança do Java.
     */
    public void injectField(Object instance, Field field, Object value) {
        boolean wasAccessible = field.canAccess(instance);

        try {
            if (!wasAccessible) {
                field.setAccessible(true);
            }
            field.set(instance, value);
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
     * Injeta múltiplos valores em seus respectivos campos de uma única instância.
     *
     * @param instance     A instância alvo.
     * @param fieldValues Um mapa associando os campos aos seus novos valores.
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
     * Invoca um método em uma instância fornecida, passando os argumentos especificados.
     *
     * <p>Contorna o modificador de acesso do método se necessário, garantindo a restauração
     * do estado original ao final da execução.</p>
     *
     * @param instance A instância na qual o método será invocado.
     * @param method   O método a ser executado.
     * @param args     Os argumentos a serem passados para o método.
     * @return O objeto retornado pela invocação do método, ou {@code null} para métodos void.
     * @throws RuntimeException Se a invocação falhar por qualquer motivo (segurança, argumentos inválidos, etc.).
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
     * Invoca uma lista de métodos em sequência na mesma instância.
     *
     * <p>Os argumentos para cada método são resolvidos dinamicamente através da função
     * fornecida no parâmetro {@code argsProvider}.</p>
     *
     * @param instance     A instância alvo.
     * @param methods      A lista de métodos a serem invocados.
     * @param argsProvider Função que recebe um método e retorna o array de argumentos adequado para ele.
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
     * Cria uma nova instância de uma classe invocando o construtor fornecido
     * com os argumentos especificados.
     *
     * <p>Contorna o modificador de acesso do construtor, permitindo a instanciação
     * de classes com construtores privados (padrão Singleton, por exemplo).</p>
     *
     * @param <T>         O tipo da instância a ser criada.
     * @param constructor O construtor a ser invocado.
     * @param args        Os argumentos necessários para a invocação do construtor.
     * @return A nova instância criada.
     * @throws RuntimeException Se a instanciação falhar.
     */
    @SuppressWarnings("unchecked")
    public <T> T instantiate(Constructor<?> constructor, Object... args) {
        boolean wasAccessible = constructor.canAccess(null);

        try {
            if (!wasAccessible) {
                constructor.setAccessible(true);
            }
            return (T) constructor.newInstance(args);
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
     * Lê o valor atual de um campo de uma instância específica.
     *
     * <p>Contorna o modificador de acesso do campo caso não seja acessível publicamente.</p>
     *
     * @param instance A instância alvo.
     * @param field    O campo a ser lido.
     * @return O valor atual armazenado no campo.
     * @throws RuntimeException Se a leitura falhar por restrições de segurança.
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
    // VERIFICAÇÃO DE ANOTAÇÕES
    // =============================================

    /**
     * Verifica se um campo possui uma anotação específica.
     *
     * @param field      O campo a ser verificado.
     * @param annotation O tipo da anotação a ser buscada.
     * @return {@code true} se a anotação estiver presente, {@code false} caso contrário.
     */
    public boolean hasAnnotation(Field field, Class<? extends java.lang.annotation.Annotation> annotation) {
        return field.isAnnotationPresent(annotation);
    }

    /**
     * Verifica se um método possui uma anotação específica.
     *
     * @param method     O método a ser verificado.
     * @param annotation O tipo da anotação a ser buscada.
     * @return {@code true} se a anotação estiver presente, {@code false} caso contrário.
     */
    public boolean hasAnnotation(Method method, Class<? extends java.lang.annotation.Annotation> annotation) {
        return method.isAnnotationPresent(annotation);
    }

    /**
     * Extrai o valor de uma propriedade específica a partir de uma anotação presente no campo.
     *
     * <p>Este método itera sobre as anotações do campo e tenta invocar o método getter
     * informado. Se a anotação não possuir tal método ou ocorrer um erro, retorna {@code null}
     * silenciosamente.</p>
     *
     * @param <T>         O tipo de retorno esperado.
     * @param field       O campo anotado.
     * @param methodName O nome do método getter na anotação (ex: "value").
     * @return O valor da propriedade, ou {@code null} se indisponível.
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