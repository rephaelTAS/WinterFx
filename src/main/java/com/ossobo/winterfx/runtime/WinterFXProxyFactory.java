// WinterFXProxyFactory.java v7.0 - 2026-08-22
// Fábrica de proxies com Fail-Fast, DRY e delegação ao PipelineExecutor
package com.ossobo.winterfx.runtime;

import com.ossobo.winterfx.anotations.Intercepted;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;
import com.ossobo.winterfx.runtime.pipeline.PipelineExecutor;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * WinterFXProxyFactory v7.0 — FAIL-FAST + DRY + DELEGAÇÃO
 *
 * <p>Cria proxies ByteBuddy que interceptam métodos anotados com {@code @Intercepted}
 * e delegam AO {@link PipelineExecutor} para orquestração.</p>
 *
 * <p><b>Mudanças:</b></p>
 * <ul>
 *   <li>✅ FAIL-FAST: exceções não são engolidas (anti-pattern Bean Zumbi eliminado)</li>
 *   <li>✅ DRY: delega ao PipelineExecutor (código morto removido)</li>
 *   <li>✅ PipelineExecutor agora é usado no lugar da lógica reescrita</li>
 * </ul>
 *
 * @version 7.0 (22/08/2026)
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
     * @throws IllegalStateException Se falhar ao criar proxy (FAIL-FAST)
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

                        // DELEGA AO PIPELINE EXECUTOR (DRY)
                        return pipelineExecutor.execute(original, targetMethod, args);

                    }))
                    .make()
                    .load(targetClass.getClassLoader())
                    .getLoaded()
                    .getDeclaredConstructor()
                    .newInstance();

        } catch (Exception e) {
            // FAIL-FAST: NUNCA engolir exceção (anti-pattern Bean Zumbi)
            throw new IllegalStateException(
                    "Falha crítica ao criar proxy WinterFX para " + targetClass.getName() +
                            ". Verifique dependências e permissões de módulo.",
                    e
            );
        }
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