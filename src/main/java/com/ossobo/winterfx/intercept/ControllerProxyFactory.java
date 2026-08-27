// ControllerProxyFactory.java v4.0 - 2026-08-22
// Fábrica de proxies JDK com delegação ao PipelineExecutor (DRY)
package com.ossobo.winterfx.intercept;

import com.ossobo.winterfx.runtime.HandlerRegistry;
import com.ossobo.winterfx.runtime.pipeline.PipelineExecutor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Objects;

/**
 * ControllerProxyFactory v4.0 — DESACOPLADO + DRY + DELEGAÇÃO
 *
 * <p>Fábrica de proxies JDK para controllers.</p>
 *
 * <p>NÃO conhece anotações específicas nem módulos externos.
 * Delega TODA a lógica de interceptação ao {@link PipelineExecutor}.</p>
 *
 * <p><b>Princípios aplicados:</b></p>
 * <ul>
 *   <li>✅ DRY: lógica do pipeline centralizada no PipelineExecutor</li>
 *   <li>✅ SRP: ControllerProxyFactory é apenas um adaptador de tecnologia</li>
 *   <li>✅ FAIL-FAST: validação rigorosa de parâmetros</li>
 *   <li>✅ JPMS-Safe: delega ao PipelineExecutor que já trata canAccess()</li>
 *   <li>✅ Java 17+ idioms: method.getDeclaringClass() == Object.class</li>
 * </ul>
 *
 * @version 4.0 (22/08/2026) - Delegação ao PipelineExecutor
 */
public final class ControllerProxyFactory {

    private final HandlerRegistry handlerRegistry;
    private final PipelineExecutor pipelineExecutor;

    public ControllerProxyFactory(HandlerRegistry handlerRegistry) {
        this.handlerRegistry = Objects.requireNonNull(handlerRegistry, "handlerRegistry não pode ser null");
        this.pipelineExecutor = new PipelineExecutor(handlerRegistry);
    }

    /**
     * Cria um proxy JDK para o controller.
     *
     * @param original            Controller original (não pode ser null)
     * @param controllerInterface Interface que o controller implementa (não pode ser null)
     * @param <T>                 Tipo do controller
     * @return Proxy ou o próprio original se não houver handlers
     * @throws NullPointerException Se original ou controllerInterface for null
     * @throws IllegalArgumentException Se controllerInterface não for uma interface válida
     * @throws IllegalStateException Se falhar ao criar proxy
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(T original, Class<?> controllerInterface) {
        // FAIL-FAST: validação rigorosa
        Objects.requireNonNull(original, "original não pode ser null");
        Objects.requireNonNull(controllerInterface, "controllerInterface não pode ser null");

        // Verifica se a interface é realmente uma interface
        if (!controllerInterface.isInterface()) {
            throw new IllegalArgumentException(
                    "controllerInterface deve ser uma interface: " + controllerInterface.getName()
            );
        }

        // Verifica se o controller implementa a interface
        if (!controllerInterface.isAssignableFrom(original.getClass())) {
            throw new IllegalArgumentException(
                    String.format(
                            "Controller %s não implementa a interface %s",
                            original.getClass().getName(),
                            controllerInterface.getName()
                    )
            );
        }

        // Se não há handlers registrados para esta classe, não precisa de proxy
        if (!handlerRegistry.hasHandlers(original.getClass())) {
            return original;
        }

        try {
            return (T) Proxy.newProxyInstance(
                    original.getClass().getClassLoader(),
                    new Class<?>[]{controllerInterface},
                    new ControllerInvocationHandler(original, pipelineExecutor, handlerRegistry)
            );
        } catch (Exception e) {
            // FAIL-FAST: NUNCA engolir exceção
            throw new IllegalStateException(
                    "Falha crítica ao criar proxy JDK para " + original.getClass().getName() +
                            " com interface " + controllerInterface.getName(),
                    e
            );
        }
    }

    /**
     * Versão com múltiplas interfaces.
     *
     * @param original Controller original
     * @param interfaces Interfaces que o controller implementa
     * @param <T> Tipo do controller
     * @return Proxy ou original
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(T original, Class<?>... interfaces) {
        Objects.requireNonNull(original, "original não pode ser null");
        Objects.requireNonNull(interfaces, "interfaces não pode ser null");

        if (interfaces.length == 0) {
            throw new IllegalArgumentException("Pelo menos uma interface deve ser fornecida");
        }

        // Valida todas as interfaces
        for (var iface : interfaces) {
            if (!iface.isInterface()) {
                throw new IllegalArgumentException("Todos os elementos devem ser interfaces: " + iface.getName());
            }
            if (!iface.isAssignableFrom(original.getClass())) {
                throw new IllegalArgumentException(
                        String.format("Controller %s não implementa a interface %s",
                                original.getClass().getName(), iface.getName())
                );
            }
        }

        if (!handlerRegistry.hasHandlers(original.getClass())) {
            return original;
        }

        try {
            return (T) Proxy.newProxyInstance(
                    original.getClass().getClassLoader(),
                    interfaces,
                    new ControllerInvocationHandler(original, pipelineExecutor, handlerRegistry)
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Falha crítica ao criar proxy JDK para " + original.getClass().getName() +
                            " com interfaces " + Arrays.toString(interfaces),
                    e
            );
        }
    }

    // ============================================================
    // INVOCATION HANDLER (Delega ao PipelineExecutor)
    // ============================================================

    private static class ControllerInvocationHandler implements InvocationHandler {

        private final Object original;
        private final PipelineExecutor executor;
        private final HandlerRegistry registry;

        ControllerInvocationHandler(Object original, PipelineExecutor executor, HandlerRegistry registry) {
            this.original = Objects.requireNonNull(original, "original não pode ser null");
            this.executor = Objects.requireNonNull(executor, "executor não pode ser null");
            this.registry = Objects.requireNonNull(registry, "registry não pode ser null");
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Java 17+ idiomático: verifica se é método da classe base Object
            // Substitui a checagem frágil com Strings ("toString", "hashCode", etc)
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(original, args);
            }

            // Encontra o método real na classe original
            Method targetMethod = findTargetMethod(method);

            // Se não há handlers para este método, executa direto (sem custo de proxy)
            if (!registry.hasHandlers(targetMethod)) {
                return method.invoke(original, args);
            }

            // DRY TOTAL: Delega ao PipelineExecutor.
            // Isso garante JPMS-Safe, Fases, Fail-Fast e tratamento de erros idênticos ao ByteBuddy.
            return executor.execute(original, targetMethod, args);
        }

        /**
         * Encontra o método real na classe original.
         *
         * <p><b>FAIL-FAST:</b> Se o método da interface não existe no original,
         * lança exceção imediatamente em vez de retornar o método do proxy.</p>
         */
        private Method findTargetMethod(Method proxyMethod) {
            try {
                return original.getClass().getMethod(
                        proxyMethod.getName(),
                        proxyMethod.getParameterTypes()
                );
            } catch (NoSuchMethodException e) {
                // FAIL-FAST: se o método da interface não existe no original,
                // é erro de contrato - não deve ser ignorado
                throw new IllegalStateException(
                        String.format(
                                "Método '%s' declarado na interface não encontrado na implementação %s. " +
                                        "Verifique se a assinatura do método está correta.",
                                proxyMethod.getName(),
                                original.getClass().getName()
                        ),
                        e
                );
            }
        }
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public HandlerRegistry getHandlerRegistry() {
        return handlerRegistry;
    }

    public PipelineExecutor getPipelineExecutor() {
        return pipelineExecutor;
    }
}