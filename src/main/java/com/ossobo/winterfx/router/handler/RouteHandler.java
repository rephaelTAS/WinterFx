package com.ossobo.winterfx.router.handler;


import com.ossobo.winterfx.anotations.UiMapping;
import com.ossobo.winterfx.router.model.ResponseData;
import com.ossobo.winterfx.router.model.RouteType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;

/**
 * Registro imutável de um handler de rota no WinterFX.
 *
 * <p>Representa o acoplamento entre:</p>
 * <ul>
 *   <li>a instância do controller (bean gerenciado pelo DI),</li>
 *   <li>o método alvo anotado,</li>
 *   <li>e o canal exato em que foi registrado ({@link RouteType} + caminho canônico).</li>
 * </ul>
 *
 * <p>Responsabilidades assumidas por esta classe (antes espalhadas no Dispatcher):</p>
 * <ul>
 *   <li><b>Validação de contrato</b> no momento da criação (não-static, retorna ResponseData);</li>
 *   <li><b>Acessibilidade resolvida uma única vez</b> (no registro, não a cada despacho);</li>
 *   <li><b>Cache dos parâmetros</b> e do flag {@code requiresFxThread};</li>
 *   <li><b>Execução reflexiva segura</b>, desembrulhando corretamente a
 *       {@link InvocationTargetException} para preservar a exceção original do controller.</li>
 * </ul>
 */
public final class RouteHandler {

    private final Object bean;
    private final Method method;
    private final RouteType type;
    private final String path;
    private final Parameter[] parameters;
    private final boolean requiresFxThread;

    // =====================================================================
    // CONSTRUÇÃO E VALIDAÇÃO (fail-fast no boot)
    // =====================================================================

    private RouteHandler(Object bean, Method method, RouteType type, String path) {
        this.bean  = Objects.requireNonNull(bean,  "bean não pode ser nulo");
        this.method = Objects.requireNonNull(method, "method não pode ser nulo");
        this.type  = Objects.requireNonNull(type,  "type não pode ser nulo");
        this.path  = Objects.requireNonNull(path,  "path não pode ser nulo");

        // Acessibilidade resolvida UMA vez no registro (e não por despacho)
        this.method.setAccessible(true);

        this.parameters = method.getParameters();

        // Cache: rotas UI exigem FX Thread; rotas que injetam @UI também.
        this.requiresFxThread = (type == RouteType.UI)
                || Arrays.stream(parameters)
                .anyMatch(p -> p.isAnnotationPresent(UiMapping.class));
    }

    /**
     * Fábrica que valida o contrato antes de instanciar.
     *
     * @throws IllegalStateException se o método violar o contrato do framework
     */
    public static RouteHandler of(Object bean, Method method, RouteType type, String path) {
        validate(bean.getClass(), method);
        return new RouteHandler(bean, method, type, path);
    }

    private static void validate(Class<?> clazz, Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException(String.format(
                    "%s.%s: handlers não podem ser static — devem pertencer a beans gerenciados pelo DI.",
                    clazz.getSimpleName(), method.getName()));
        }
        if (!ResponseData.class.isAssignableFrom(method.getReturnType())) {
            throw new IllegalStateException(String.format(
                    "%s.%s: todo handler deve retornar ResponseData (retorno atual: %s).",
                    clazz.getSimpleName(), method.getName(),
                    method.getReturnType().getSimpleName()));
        }
    }

    // =====================================================================
    // EXECUÇÃO
    // =====================================================================

    /**
     * Invoca o método alvo com os argumentos já resolvidos pelo Dispatcher.
     *
     * <p>Exceções do controller são relançadas como-is (se RuntimeException/Error),
     * preservando o stacktrace original; checked viram RuntimeException com a causa intacta.</p>
     */
    public Object invoke(Object... args) {
        try {
            return method.invoke(bean, args);
        } catch (InvocationTargetException e) {
            var cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err)          throw err;
            throw new RuntimeException(
                    "Falha na rota [" + type + "] '" + path + "' (" + signature() + ")", cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Falha reflexiva na rota [" + type + "] '" + path + "' (" + signature() + ")", e);
        }
    }

    /** Indica se esta rota só pode executar na JavaFX Application Thread. */
    public boolean requiresFxThread() {
        return requiresFxThread;
    }

    // =====================================================================
    // ACESSORES
    // =====================================================================

    public Object bean()         { return bean; }
    public Method method()       { return method; }
    public RouteType type()      { return type; }
    public String path()         { return path; }

    /** Cópia defensiva dos parâmetros. */
    public Parameter[] parameters() { return parameters.clone(); }

    /** Identificador legível para logs e mensagens de erro (ex: "LivroController#salvar"). */
    public String signature() {
        return bean.getClass().getSimpleName() + "#" + method.getName();
    }

    @Override
    public String toString() {
        return "[" + type + "] " + path + " -> " + signature();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteHandler other)) return false;
        return bean == other.bean
                && method.equals(other.method)
                && type == other.type
                && path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(bean), method, type, path);
    }
}