package com.ossobo.winterfx.router.processor;

import com.ossobo.winterfx.anotations.DeleteMapping;
import com.ossobo.winterfx.anotations.ExecMapping;
import com.ossobo.winterfx.anotations.GetMapping;
import com.ossobo.winterfx.anotations.PutMapping;
import com.ossobo.winterfx.anotations.RequestMapping;
import com.ossobo.winterfx.anotations.UI;
import com.ossobo.winterfx.anotations.UiMapping;
import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.router.model.*;
import javafx.application.Platform;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * ApiDispatcher v6.1 — Despachante central com 5 namespaces e
 * COMPARTILHAMENTO CONTROLADO de rotas.
 *
 * <h2>Estrutura interna</h2>
 * <pre>
 * routes: Map&lt;path, EnumMap&lt;RouteType, RouteHandler&gt;&gt;
 *
 * "/livros/exportar-csv" ──┬── GET     → LivroController#exportarCsv
 *                          ├── EXECUTE → LivroController#exportarCsv   (mesmo método!)
 *                          └── UI      → (se anotado)
 * </pre>
 *
 * <h2>Regras de segurança</h2>
 * <ul>
 *   <li>O lookup NUNCA cruza verbos: {@code Rotas.get()} consulta somente o canal GET.</li>
 *   <li>{GET, PUT, DELETE} são mutuamente exclusivos NA MESMA ROTA — tentativa
 *       de mistura falha no boot com {@link IllegalStateException}.</li>
 *   <li>{EXEC, UI} são livres para combinar com qualquer verbo, pois possuem
 *       canais próprios e semântica distinta (comando / transporte visual).</li>
 *   <li>Rota do canal UI exige JavaFX Application Thread (fail-fast fora dela).
 *       Rotas de outros canais que INJETEM parâmetros {@code @UI} também exigem.</li>
 * </ul>
 *
 * <h2>Divisão de papéis das anotações</h2>
 * <table>
 *   <tr><th>Anotação</th><th>Alvo</th><th>Consumida por</th></tr>
 *   <tr><td>{@code @GetMapping/@PutMapping/@DeleteMapping/@ExecMapping/@UiMapping}</td>
 *       <td>METHOD</td><td>Registro (este Dispatcher)</td></tr>
 *   <tr><td>{@code @UI}</td><td>PARAMETER</td><td>Binding ({@code UIResolver})</td></tr>
 * </table>
 */
public class ApiDispatcher {

    private static final System.Logger LOGGER = System.getLogger(ApiDispatcher.class.getName());

    /** Verbos de dados — mutuamente exclusivos entre si na mesma rota. */
    private static final Set<RouteType> DATA_VERBS =
            Set.of(RouteType.GET, RouteType.PUT, RouteType.DELETE);

    /** Tradução centralizada: anotação → canal. Único ponto de verdade. */
    private static final List<Map.Entry<Class<? extends Annotation>, RouteType>> MAPPINGS = List.of(
            Map.entry(GetMapping.class,    RouteType.GET),
            Map.entry(PutMapping.class,    RouteType.PUT),
            Map.entry(DeleteMapping.class, RouteType.DELETE),
            Map.entry(ExecMapping.class,   RouteType.EXECUTE),
            Map.entry(UiMapping.class,     RouteType.UI)
    );

    /**
     * Tabela de rotas: para cada caminho canônico, um submapa por verbo.
     * É isso que permite "/x/y" existir simultaneamente em GET + EXEC + UI,
     * mantendo cada canal fisicamente isolado.
     */
    private final Map<String, EnumMap<RouteType, RouteHandler>> routes = new ConcurrentHashMap<>();

    private final DiContainer container;
    private final List<ParameterResolver> resolvers;

    private static final Object[] NO_ARGS = {};

    public ApiDispatcher(DiContainer container) {
        this.container = container;
        this.resolvers = List.of(
                new RouteVarResolver(),
                new PayloadResolver(),
                new UIResolver()
        );
        scanControllers();
    }

    // =====================================================================
    // REGISTRO
    // =====================================================================

    private void scanControllers() {
        for (var definition : container.getBeanRegistry().getAllDefinitions()) {
            var clazz = definition.type();
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                register(container.getBean(clazz));
            }
        }

        LOGGER.log(System.Logger.Level.INFO,
                "WinterFX Router iniciado — {0} registro(s): get={1}, put={2}, delete={3}, exec={4}, ui={5}",
                entryCount(),
                count(RouteType.GET), count(RouteType.PUT), count(RouteType.DELETE),
                count(RouteType.EXECUTE), count(RouteType.UI));
    }

    private void register(Object bean) {
        var clazz = bean.getClass();
        var prefix = clazz.getAnnotation(RequestMapping.class).value();

        for (var method : findAnnotatedMethods(clazz)) {
            validateHandlerMethod(clazz, method);
            validateVerbCompatibility(clazz, method);   // ★ regra GET/PUT/DELETE por método

            var handler = new RouteHandler(bean, method);

            // ★ CADA anotação presente gera SEU PRÓPRIO registro —
            //   permitindo o compartilhamento: @GetMapping + @ExecMapping no mesmo método.
            for (var mapping : MAPPINGS) {
                if (!method.isAnnotationPresent(mapping.getKey())) continue;

                var rawPath = readValue(method.getAnnotation(mapping.getKey()));
                var path = canonicalize(joinPath(prefix, rawPath));

                addRoute(path, mapping.getValue(), handler, clazz);
            }
        }
    }

    /**
     * Regra por MÉTODO: nunca misturar dois verbos de dados no mesmo handler
     * (evita acoplamento indevido e encoraja telas/ações separadas).
     */
    private void validateVerbCompatibility(Class<?> clazz, Method method) {
        var dataVerbsFound = new ArrayList<String>();

        if (method.isAnnotationPresent(GetMapping.class))    dataVerbsFound.add("@GetMapping");
        if (method.isAnnotationPresent(PutMapping.class))    dataVerbsFound.add("@PutMapping");
        if (method.isAnnotationPresent(DeleteMapping.class)) dataVerbsFound.add("@DeleteMapping");

        if (dataVerbsFound.size() > 1) {
            throw new IllegalStateException(String.format(
                    "%s.%s: verbos de dados são mutuamente exclusivos. Encontrados: %s. " +
                            "Use @ExecMapping ou @UiMapping para compartilhar a rota.",
                    clazz.getSimpleName(), method.getName(), dataVerbsFound));
        }
    }

    /**
     * Insere um registro validando DUAS regras:
     * 1. (path, verbo) duplicado → conflito explícito com nome dos métodos;
     * 2. Mistura de DATA_VERBS (GET/PUT/DELETE) no mesmo path → bloqueada,
     *    independente de estarem no mesmo método ou em métodos diferentes.
     */
    private void addRoute(String path, RouteType type, RouteHandler handler, Class<?> clazz) {
        var verbMap = routes.computeIfAbsent(path, k -> new EnumMap<>(RouteType.class));

        var existing = verbMap.putIfAbsent(type, handler);
        if (existing != null) {
            throw new IllegalStateException(String.format(
                    "Rota duplicada %s %s: %s.%s conflita com %s.%s",
                    type, path,
                    clazz.getSimpleName(), handler.method().getName(),
                    existing.bean().getClass().getSimpleName(),
                    existing.method().getName()));
        }

        var dataVerbsOnPath = verbMap.keySet().stream()
                .filter(DATA_VERBS::contains)
                .sorted()
                .toList();

        if (dataVerbsOnPath.size() > 1) {
            throw new IllegalStateException(String.format(
                    "VIOLAÇÃO DE SEGURANÇA na rota '%s': mistura de verbos de dados %s. " +
                            "GET, PUT e DELETE não podem compartilhar a mesma rota entre si " +
                            "(EXEC e UI são livres).",
                    path, dataVerbsOnPath));
        }
    }

    private void validateHandlerMethod(Class<?> clazz, Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException(String.format(
                    "%s.%s: handlers não podem ser static.", clazz.getSimpleName(), method.getName()));
        }
        if (!ResponseData.class.isAssignableFrom(method.getReturnType())) {
            throw new IllegalStateException(String.format(
                    "%s.%s: todo handler deve retornar ResponseData (retorno atual: %s).",
                    clazz.getSimpleName(), method.getName(),
                    method.getReturnType().getSimpleName()));
        }
    }

    /** Varre a hierarquia de classes, deduplicando overrides. */
    private List<Method> findAnnotatedMethods(Class<?> clazz) {
        var found = new ArrayList<Method>();
        var seenSignatures = new HashSet<String>();

        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (var method : c.getDeclaredMethods()) {
                var isRoute = MAPPINGS.stream().anyMatch(m -> method.isAnnotationPresent(m.getKey()));
                if (!isRoute) continue;

                var signature = method.getName() + Arrays.toString(method.getParameterTypes());
                if (seenSignatures.add(signature)) found.add(method);
            }
        }
        return found;
    }

    // =====================================================================
    // DESPACHO — UM MÉTODO PÚBLICO POR CANAL
    // =====================================================================

    public Object dispatchGet(String rota, Map<String, Object> params)    { return executeStrict(RouteType.GET, rota, params); }
    public Object dispatchPut(String rota, Map<String, Object> params)    { return executeStrict(RouteType.PUT, rota, params); }
    public Object dispatchDelete(String rota, Map<String, Object> params) { return executeStrict(RouteType.DELETE, rota, params); }
    public Object dispatchExec(String rota, Map<String, Object> params)   { return executeStrict(RouteType.EXECUTE, rota, params); }

    /**
     * Canal UI: transporte de componentes visuais entre pontos distintos
     * da aplicação. Exige obrigatoriamente a JavaFX Application Thread.
     */
    public Object dispatchUi(String rota, Map<String, Object> params) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(String.format(
                    "Rota UI '%s' exige a JavaFX Application Thread. " +
                            "Use Platform.runLater(() -> Rotas.ui(...)) ou Rotas.onFxThread(() -> ...).",
                    rota));
        }
        return executeStrict(RouteType.UI, rota, params);
    }

    /** Atalhos sem parâmetros. */
    public Object dispatchGet(String rota)    { return dispatchGet(rota, Map.of()); }
    public Object dispatchPut(String rota)    { return dispatchPut(rota, Map.of()); }
    public Object dispatchDelete(String rota) { return dispatchDelete(rota, Map.of()); }
    public Object dispatchExec(String rota)   { return dispatchExec(rota, Map.of()); }
    public Object dispatchUi(String rota)     { return dispatchUi(rota, Map.of()); }

    // =====================================================================
    // DESPACHO POSICIONAL (legado)
    // =====================================================================

    public Object dispatchGet(String rota, Object... positionalArgs)     { return positional(RouteType.GET, rota, positionalArgs); }
    public Object dispatchPut(String rota, Object... positionalArgs)     { return positional(RouteType.PUT, rota, positionalArgs); }
    public Object dispatchDelete(String rota, Object... positionalArgs)  { return positional(RouteType.DELETE, rota, positionalArgs); }
    public Object dispatchExec(String rota, Object... positionalArgs)    { return positional(RouteType.EXECUTE, rota, positionalArgs); }
    public Object dispatchUi(String rota, Object... positionalArgs)      { return positional(RouteType.UI, rota, positionalArgs); }

    private Object positional(RouteType type, String rota, Object[] args) {
        var handler = lookup(type, rota);
        if (handler == null) return missing(type, rota);
        ensureUiThreadRule(handler, type, rota);
        return invoke(handler, args == null ? NO_ARGS : args, type, rota);
    }

    // =====================================================================
    // NÚCLEO
    // =====================================================================

    private Object executeStrict(RouteType type, String rota, Map<String, Object> params) {
        Objects.requireNonNull(rota, "rota não pode ser nula");

        var handler = lookup(type, rota);
        if (handler == null) return missing(type, rota);

        ensureUiThreadRule(handler, type, rota);

        try {
            makeAccessible(handler);
            var request = new RouteRequest(canonicalize(rota), tolerantCopy(params));
            var resolvedArgs = resolveArgs(handler.method(), request);
            return invoke(handler, resolvedArgs, type, rota);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Falha no binding da rota [" + type + "] '" + rota + "': " + e.getMessage(), e);
        }
    }

    /**
     * Lookup ESTRICTO por canal: entra no submapa do path e lê APENAS
     * a entrada do verbo solicitado. Um canal jamais enxerga o outro.
     */
    private RouteHandler lookup(RouteType type, String rota) {
        var verbMap = routes.get(canonicalize(rota));
        return (verbMap == null) ? null : verbMap.get(type);
    }

    /**
     * Regra da FX Thread aplicada em dois casos:
     * 1. A rota pertence ao canal UI (registrada via {@code @UiMapping}); OU
     * 2. Qualquer parâmetro do handler injeta um componente visual
     *    via {@code @UI} — o que exige a FX Thread independente do canal.
     *
     * <p>Note que aqui usamos {@code @UI} (PARAMETER), nunca {@code @UiMapping}
     * (METHOD): esta última jamais pode aparecer em parâmetros.</p>
     */
    private void ensureUiThreadRule(RouteHandler handler, RouteType type, String rota) {
        boolean needsFxThread = (type == RouteType.UI)
                || Arrays.stream(handler.method().getParameters())
                .anyMatch(p -> p.isAnnotationPresent(UI.class));   // ★ UI, não UiMapping!

        if (needsFxThread && !Platform.isFxApplicationThread()) {
            throw new IllegalStateException(String.format(
                    "A rota [%s] '%s' manipula componentes visuais e DEVE rodar na " +
                            "JavaFX Application Thread.",
                    type, rota));
        }
    }

    private ResponseData missing(RouteType type, String rota) {
        return ResponseData.error("Rota inexistente [" + type + "]: " + canonicalize(rota));
    }

    private Object invoke(RouteHandler handler, Object[] args, RouteType type, String rota) {
        try {
            return handler.method().invoke(handler.bean(), args);
        } catch (InvocationTargetException e) {
            var cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err) throw err;
            throw new RuntimeException("Falha na rota [" + type + "] '" + rota + "'", cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Falha reflexiva em [" + type + "] '" + rota + "'", e);
        }
    }

    private void makeAccessible(RouteHandler handler) {
        if (!handler.method().canAccess(handler.bean())) {
            handler.method().setAccessible(true);
        }
    }

    private Object[] resolveArgs(Method method, RouteRequest request) {
        var parameters = method.getParameters();
        var args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            var param = parameters[i];
            var resolved = false;

            for (var resolver : resolvers) {
                if (resolver.supports(param)) {
                    args[i] = resolver.resolve(param, request);
                    resolved = true;
                    break;
                }
            }
            if (!resolved) {
                throw new IllegalArgumentException(
                        "Parâmetro '" + param.getName() + "' em '" + method.getName()
                                + "' sem anotação suportada (@Payload, @UI ou @RouteVar).");
            }
        }
        return args;
    }

    // =====================================================================
    // AÇÃO AD-HOC
    // =====================================================================

    public Object dispatchAction(String rota, String actionName) {
        Objects.requireNonNull(actionName);
        var handler = locateAnyChannel(rota);
        if (handler == null) return ResponseData.error("Rota inexistente: " + canonicalize(rota));

        Method target = null;
        for (Class<?> c = handler.bean().getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try { target = c.getDeclaredMethod(actionName); break; }
            catch (NoSuchMethodException ignored) {}
        }
        if (target == null) {
            return ResponseData.error("Método '" + actionName + "' não encontrado em: " + rota);
        }
        try {
            if (!target.canAccess(handler.bean())) target.setAccessible(true);
            return target.invoke(handler.bean());
        } catch (InvocationTargetException e) {
            var cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err) throw err;
            throw new RuntimeException("Erro na ação '" + actionName + "'", cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Erro reflexivo na ação '" + actionName + "'", e);
        }
    }

    private RouteHandler locateAnyChannel(String rota) {
        var verbMap = routes.get(canonicalize(rota));
        return (verbMap == null || verbMap.isEmpty())
                ? null : verbMap.values().iterator().next();
    }

    // =====================================================================
    // UTILITÁRIOS
    // =====================================================================

    public static <T> T onFxThread(Supplier<T> action) {
        Objects.requireNonNull(action);
        if (Platform.isFxApplicationThread()) return action.get();
        var future = new CompletableFuture<T>();
        Platform.runLater(() -> {
            try { future.complete(action.get()); }
            catch (Throwable t) { future.completeExceptionally(t); }
        });
        return future.join();
    }

    public void clear() { routes.clear(); }

    public int getRouteCount() { return entryCount(); }

    /** Quantidade de registros em um canal específico. */
    public int count(RouteType type) {
        return routes.values().stream().mapToInt(vm -> vm.containsKey(type) ? 1 : 0).sum();
    }

    private int entryCount() {
        return routes.values().stream().mapToInt(Map::size).sum();
    }

    /**
     * Diagnóstico: mostra caminhos com TODOS os verbos agrupados,
     * evidenciando o compartilhamento legítimo (GET+EXEC, etc).
     */
    public String dumpRoutes() {
        var sb = new StringBuilder("=== WINTERFX ROUTES ===\n");
        routes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    var verbs = e.getValue().keySet().stream()
                            .sorted(Enum::compareTo)
                            .map(t -> "[" + t + "]")
                            .reduce((a, b) -> a + b)
                            .orElse("");
                    var anyHandler = e.getValue().values().iterator().next();
                    sb.append(String.format("%-50s %s -> %s#%s%n",
                            e.getKey(), String.format("%-22s", verbs),
                            anyHandler.bean().getClass().getSimpleName(),
                            anyHandler.method().getName()));
                    // lista todos os pares verbo->método quando divergem
                    e.getValue().forEach((t, h) -> {
                        if (h.method() != anyHandler.method()) {
                            sb.append(String.format("%-73s ^ [%s] aponta para %s#%s%n",
                                    "", t, h.bean().getClass().getSimpleName(), h.method().getName()));
                        }
                    });
                });
        return sb.toString();
    }

    // =====================================================================
    // HELPERS DE CAMINHO
    // =====================================================================

    private String readValue(Annotation mapping) {
        try {
            return (String) mapping.annotationType().getMethod("value").invoke(mapping);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Anotação sem 'value()': " + mapping, e);
        }
    }

    private static String joinPath(String prefix, String path) {
        var p = trimSlashes(prefix == null ? "" : prefix);
        var s = trimSlashes(path == null ? "" : path);
        if (p.isEmpty()) return s;
        if (s.isEmpty()) return p;
        return p + "/" + s;
    }

    static String canonicalize(String path) {
        var trimmed = trimSlashes(Objects.requireNonNull(path));
        return trimmed.isEmpty() ? "/" : "/" + trimmed;
    }

    private static String trimSlashes(String value) {
        var r = value;
        while (r.startsWith("/")) r = r.substring(1);
        while (r.endsWith("/"))   r = r.substring(0, r.length() - 1);
        return r;
    }

    private static Map<String, Object> tolerantCopy(Map<String, Object> source) {
        return (source == null || source.isEmpty()) ? Map.of() : new LinkedHashMap<>(source);
    }
}