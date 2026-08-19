package com.ossobo.winterfx.router.processor;

import com.ossobo.winterfx.anotations.GetMapping;
import com.ossobo.winterfx.anotations.PostMapping;
import com.ossobo.winterfx.anotations.RequestMapping;
import com.ossobo.winterfx.anotations.UI;
import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.router.model.ParameterResolver;
import com.ossobo.winterfx.router.model.PayloadResolver;
import com.ossobo.winterfx.router.model.ResponseData;
import com.ossobo.winterfx.router.model.RouteRequest;
import com.ossobo.winterfx.router.model.RouteVarResolver;
import com.ossobo.winterfx.router.model.UIResolver;
import javafx.application.Platform;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Despachante central (Internal Router) com suporte a resolução semântica de parâmetros.
 *
 * <p>Além do roteamento baseado em strings, este despachante é capaz de casar
 * um mapa de parâmetros ({@link java.util.Map}) enviado pela UI com a assinatura exata
 * do método do Controller, utilizando as anotações {@code @RouteVar}, {@code @UI}
 * e {@code @Payload} como guias de ligação.</p>
 */
public class ApiDispatcher {

    private final Map<String, RouteHandler> getRoutes = new HashMap<>();
    private final Map<String, RouteHandler> postRoutes = new HashMap<>();
    private final DiContainer container;

    // 1. Lista de resolvedores de parâmetros
    private final List<ParameterResolver> resolvers;

    public ApiDispatcher(DiContainer container) {
        this.container = container;
        // 2. Inicializa os resolvedores
        this.resolvers = List.of(
                new RouteVarResolver(),
                new PayloadResolver(),
                new UIResolver()
        );
        scanControllers();
    }

    private void scanControllers() {
        for (Object bean : container.getAllBeansOfType(Object.class)) {
            register(bean);
        }
    }

    private void register(Object bean) {
        Class<?> clazz = bean.getClass();
        if (!clazz.isAnnotationPresent(RequestMapping.class)) return;

        RequestMapping request = clazz.getAnnotation(RequestMapping.class);
        String prefix = request.value();

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GetMapping.class)) {
                String route = prefix + "/" + method.getAnnotation(GetMapping.class).value();
                getRoutes.put(route, new RouteHandler(bean, method));
            }
            if (method.isAnnotationPresent(PostMapping.class)) {
                String route = prefix + "/" + method.getAnnotation(PostMapping.class).value();
                postRoutes.put(route, new RouteHandler(bean, method));
            }
        }
    }

    /**
     * Executa o despacho utilizando reflexão posicional simples (Legado).
     */
    public Object dispatch(String rota, Object... args) {
        RouteHandler handler = getRoutes.getOrDefault(rota, postRoutes.get(rota));
        if (handler == null) return ResponseData.error("Rota inexistente: " + rota);

        try {
            Method method = handler.method();
            method.setAccessible(true);
            return method.invoke(handler.bean(), args);
        } catch (Exception e) {
            return ResponseData.error(e.getMessage());
        }
    }

    /**
     * Executa o despacho utilizando casamento semântico via Mapa de Parâmetros.
     *
     * <p>Este método lê as anotações {@code @Payload}, {@code @RouteVar} e {@code @UI}
     * dos parâmetros do método alvo, extrai os objetos correspondentes do mapa
     * e monta o array de argumentos na ordem correta exigida pela reflexão.</p>
     *
     * @param rota    O caminho da rota.
     * @param params O mapa de parâmetros construído via {@link com.ossobo.winterfx.router.model.Params}.
     * @return O resultado da invocação do método.
     */
    public Object dispatch(String rota, Map<String, Object> params) {
        RouteHandler handler = getRoutes.getOrDefault(rota, postRoutes.get(rota));
        if (handler == null) return ResponseData.error("Rota inexistente: " + rota);

        Method method = handler.method();

        // Proteção para o JavaFX: Se houver parâmetros @UI e não estivermos na JavaFX Thread,
        // despachamos para a Thread correta para evitar exceções de UI.
        boolean hasUiParam = Arrays.stream(method.getParameters())
                .anyMatch(p -> p.isAnnotationPresent(UI.class));

        if (hasUiParam && !Platform.isFxApplicationThread()) {
            Platform.runLater(() -> dispatch(rota, params));
            return ResponseData.success(); // Retorna sucesso genérico para não bloquear a thread atual
        }

        try {
            method.setAccessible(true);

            // 3. Passa o request para os resolvers
            RouteRequest request = new RouteRequest(rota, params);
            Object[] resolvedArgs = resolveArgs(method, request);

            return method.invoke(handler.bean(), resolvedArgs);
        } catch (Exception e) {
            return ResponseData.error(e.getMessage());
        }
    }

    /**
     * Despacha uma ação procurando o método pelo nome da string.
     * Útil para executar métodos void via roteamento dinâmico.
     */
    public Object dispatchAction(String rota, String actionName) {
        RouteHandler handler = getRoutes.getOrDefault(rota, postRoutes.get(rota));

        if (handler == null) {
            return ResponseData.error("Rota inexistente: " + rota);
        }

        try {
            // Acha o método "clear" dentro do CatalogoFormController
            Method method = handler.bean().getClass().getDeclaredMethod(actionName);
            method.setAccessible(true);

            // Executa o método (funciona mesmo se for void)
            return method.invoke(handler.bean());
        } catch (NoSuchMethodException e) {
            return ResponseData.error("Método '" + actionName + "' não encontrado no controller da rota: " + rota);
        } catch (Exception e) {
            return ResponseData.error("Erro ao executar ação: " + e.getMessage());
        }
    }

    /**
     * Faz o "casamento" (matching) entre o Mapa enviado e as anotações do método.
     */
    private Object[] resolveArgs(Method method, RouteRequest request) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            boolean resolved = false;

            // 4. Procura qual resolver suporta aquele parâmetro
            for (ParameterResolver resolver : resolvers) {
                if (resolver.supports(param)) {
                    args[i] = resolver.resolve(param, request);
                    resolved = true;
                    break;
                }
            }

            if (!resolved) {
                throw new IllegalArgumentException(
                        "Parâmetro '" + param.getName() + "' no método '" + method.getName() +
                                "' não possui anotação de roteamento suportada (@Payload, @UI ou @RouteVar)."
                );
            }
        }
        return args;
    }

    public void clear() {
        getRoutes.clear();
        postRoutes.clear();
    }
}