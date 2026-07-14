package com.ossobo.winterfx.runtime;

import com.ossobo.winterfx.anotations.Intercepted;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;
import com.ossobo.winterfx.runtime.pipeline.PipelineExecutor;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * WinterFXProxyFactory v6.0 — DESACOPLADO
 *
 * <p>Cria proxies ByteBuddy que interceptam métodos anotados com {@code @Intercepted}
 * e delegam ao {@link PipelineExecutor}.</p>
 *
 * <p>NÃO conhece anotações específicas de notificação, view ou imagem.
 * Apenas verifica {@code @Intercepted} e delega ao pipeline.</p>
 *
 * @version 6.0 (01/07/2026)
 */
public final class WinterFXProxyFactory {

    private final HandlerRegistry registry;
    private final PipelineExecutor pipelineExecutor;

    public WinterFXProxyFactory(HandlerRegistry registry) {
        this.registry = registry;
        this.pipelineExecutor = new PipelineExecutor(registry);
    }

    /**
     * Registra um handler de anotação.
     *
     * @param handler Handler a ser registrado
     */
    public <A extends Annotation> void registerHandler(AnnotationHandler<A> handler) {
        registry.register(handler);
    }

    /**
     * Envolve o objeto original com um proxy se houver handlers registrados
     * para métodos da classe.
     *
     * @param original Instância original
     * @param <T>      Tipo do bean
     * @return Proxy ou instância original
     */
    @SuppressWarnings("unchecked")
    public <T> T wrap(T original) {
        if (original == null) return null;

        Class<?> targetClass = original.getClass();

        // Se não há handlers registrados para esta classe, não precisa de proxy
        if (!registry.hasHandlers(targetClass)) {
            return original;
        }

        try {
            return (T) new ByteBuddy()
                    .subclass(targetClass)
                    .method(ElementMatchers.any()
                            .and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class))))
                    .intercept(InvocationHandlerAdapter.of((proxy, method, args) -> {
                        Method targetMethod = getTargetMethod(targetClass, method);

                        // Apenas métodos com @Intercepted passam pelo pipeline
                        if (!targetMethod.isAnnotationPresent(Intercepted.class)) {
                            return method.invoke(original, args);
                        }

                        // Delega ao pipeline (genérico — não conhece anotações)
                        return executeWithPipeline(original, targetMethod, method, args);

                    }))
                    .make()
                    .load(targetClass.getClassLoader())
                    .getLoaded()
                    .getDeclaredConstructor()
                    .newInstance();

        } catch (Exception e) {
            return original;
        }
    }

    /**
     * Executa o método através do pipeline de interceptação.
     */
    private Object executeWithPipeline(Object original, Method targetMethod,
                                       Method proxyMethod, Object[] args) throws Throwable {
        AnnotationContext ctx = new AnnotationContext(original, targetMethod, args);

        // FASE BEFORE — delega ao HandlerRegistry
        try {
            registry.executeBeforePhase(targetMethod, ctx);
        } catch (Exception e) {
            // Handler BEFORE interrompeu (ex: OnConfirmation cancelado)
            return null;
        }

        // EXECUÇÃO DO MÉTODO
        Object result;
        try {
            result = proxyMethod.invoke(original, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;

            // FASE AFTER — ERRO
            registry.executeErrorPhase(targetMethod, ctx.withError(cause));

            // Se o erro foi tratado por um handler, não relança
            if (registry.hasErrorHandlers(targetMethod)) {
                return null;
            }
            throw cause;
        }

        // FASE AFTER — SUCESSO
        registry.executeSuccessPhase(targetMethod, ctx.withResult(result));

        return result;
    }

    /**
     * Encontra o método real na classe original a partir do método do proxy.
     */
    private Method getTargetMethod(Class<?> targetClass, Method proxyMethod) {
        try {
            return targetClass.getMethod(proxyMethod.getName(), proxyMethod.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return proxyMethod;
        }
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public HandlerRegistry getRegistry() {
        return registry;
    }

    public PipelineExecutor getPipelineExecutor() {
        return pipelineExecutor;
    }
}