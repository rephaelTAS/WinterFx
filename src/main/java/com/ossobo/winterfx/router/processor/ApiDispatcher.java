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
 * ApiDispatcher v7.0 — Despachante central com 5 namespaces,
 * COMPARTILHAMENTO CONTROLADO de rotas e CONTRATO FORTE do envelope.
 *
 * <h2>Contrato de retorno</h2>
 * Todo despacho ({@code dispatchGet/put/delete/exec/ui}) devolve {@link ResponseData}.
 * A garantia vem do boot: {@link #validateHandlerMethod} rejeita handlers com outro
 * retorno, e o {@link #invoke} (único ponto de reflexão) rejeita {@code return null}.
 * Cast na view: NENHUM.
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
 *   <li>{GET, PUT, DELETE} são mutuamente exclusivos NA MESMA ROTA — mistura falha no boot.</li>
 *   <li>{EXEC, UI} combinam livremente com qualquer verbo (semântica própria).</li>
 *   <li>Rotas UI (e rotas que injetam {@code @UI}) exigem a JavaFX Application Thread —
 *       verificação pré-computada NO REGISTRO (não re-escaneada a cada despacho).</li>
 *   <li>{@code setAccessible(true)} resolvido uma única vez no registro.</li>
 *   <li>Binding de parâmetros lança {@link RouteBindingException} — nunca um
 *       {@code IllegalArgumentException} que disfarce erro do controller como erro de rota.</li>
 * </ul>
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
     * Permite "/x/y" existir simultaneamente em GET + EXEC + UI, com cada
     * canal fisicamente isolado.
     */
    private final Map<String, EnumMap<RouteType, RouteHandler>> routes = new ConcurrentHashMap<>();

    /**
     * Handlers que exigem a JavaFX Application Thread, computados NO REGISTRO:
     * canal UI, ou qualquer parâmetro anotado com {@code @UI}.
     * Consulta em O(1) no despacho — sem re-escanear anotações.
     */
    private final Set<RouteHandler> fxThreadHandlers = ConcurrentHashMap.newKeySet();

    private final DiContainer container;
    private final List<ParameterResolver> resolvers;

    private static final Object[] NO_ARGS = {};

    public ApiDispatcher(DiContainer container) {
        this.container = container;
        this.resolvers = List.of(
                new RouteVarResolver(),
                new PayloadResolver(),
                new UIResolver(),
                new AllParamsResolver()
        );
        scanControllers();
    }

    // =====================================================================
    // REGISTRO (fail-fast + pré-computação)
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

            // Acessibilidade resolvida UMA VEZ (não a cada despacho)
            handler.method().setAccessible(true);

            // FX Thread exigida se o canal for UI OU se qualquer parâmetro injeta @UI.
            // Computado UMA vez por método (não por despacho).
            var hasUiParam = Arrays.stream(method.getParameters())
                    .anyMatch(p -> p.isAnnotationPresent(UI.class));

            // ★ CADA anotação presente gera SEU PRÓPRIO registro —
            //   permitindo o compartilhamento: @GetMapping + @ExecMapping no mesmo método.
            for (var mapping : MAPPINGS) {
                if (!method.isAnnotationPresent(mapping.getKey())) continue;

                var rawPath = readValue(method.getAnnotation(mapping.getKey()));
                var path = canonicalize(joinPath(prefix, rawPath));
                var type = mapping.getValue();

                if (type == RouteType.UI || hasUiParam) {
                    fxThreadHandlers.add(handler);
                }
                addRoute(path, type, handler, clazz);
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
    // DESPACHO — UM MÉTODO PÚBLICO POR CANAL (contrato: sempre ResponseData)
    // =====================================================================

    public ResponseData dispatchGet(String rota, Map<String, Object> params)    { return executeStrict(RouteType.GET, rota, params); }
    public ResponseData dispatchPut(String rota, Map<String, Object> params)    { return executeStrict(RouteType.PUT, rota, params); }
    public ResponseData dispatchDelete(String rota, Map<String, Object> params) { return executeStrict(RouteType.DELETE, rota, params); }
    public ResponseData dispatchExec(String rota, Map<String, Object> params)   { return executeStrict(RouteType.EXECUTE, rota, params); }

    /**
     * Canal UI: transporte de componentes visuais entre pontos distintos
     * da aplicação. Exige obrigatoriamente a JavaFX Application Thread.
     */
    public ResponseData dispatchUi(String rota, Map<String, Object> params) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(String.format(
                    "Rota UI '%s' exige a JavaFX Application Thread. " +
                            "Use Platform.runLater(() -> Rotas.ui(...)) ou ApiDispatcher.onFxThread(() -> ...).",
                    rota));
        }
        return executeStrict(RouteType.UI, rota, params);
    }

    /** Atalhos sem parâmetros. */
    public ResponseData dispatchGet(String rota)    { return dispatchGet(rota, Map.of()); }
    public ResponseData dispatchPut(String rota)    { return dispatchPut(rota, Map.of()); }
    public ResponseData dispatchDelete(String rota) { return dispatchDelete(rota, Map.of()); }
    public ResponseData dispatchExec(String rota)   { return dispatchExec(rota, Map.of()); }
    public ResponseData dispatchUi(String rota)     { return dispatchUi(rota, Map.of()); }

    // =====================================================================
    // DESPACHO POSICIONAL (legado — prefira Params)
    // =====================================================================

    public ResponseData dispatchGet(String rota, Object... positionalArgs)     { return positional(RouteType.GET, rota, positionalArgs); }
    public ResponseData dispatchPut(String rota, Object... positionalArgs)     { return positional(RouteType.PUT, rota, positionalArgs); }
    public ResponseData dispatchDelete(String rota, Object... positionalArgs)  { return positional(RouteType.DELETE, rota, positionalArgs); }
    public ResponseData dispatchExec(String rota, Object... positionalArgs)    { return positional(RouteType.EXECUTE, rota, positionalArgs); }
    public ResponseData dispatchUi(String rota, Object... positionalArgs)      { return positional(RouteType.UI, rota, positionalArgs); }

    private ResponseData positional(RouteType type, String rota, Object[] args) {
        var handler = lookup(type, rota);
        if (handler == null) return missing(type, rota);
        ensureUiThreadRule(handler, type, rota);
        return invoke(handler, args == null ? NO_ARGS : args, type, rota);
    }

    // =====================================================================
    // NÚCLEO
    // =====================================================================

    private ResponseData executeStrict(RouteType type, String rota, Map<String, Object> params) {
        Objects.requireNonNull(rota, "rota não pode ser nula");

        var handler = lookup(type, rota);
        if (handler == null) return missing(type, rota);

        ensureUiThreadRule(handler, type, rota);

        var request = new RouteRequest(canonicalize(rota), tolerantCopy(params));
        var resolvedArgs = resolveArgs(handler, request);
        return invoke(handler, resolvedArgs, type, rota);
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
     * Regra da FX Thread — decisão JÁ COMPUTADA no registro ({@link #fxThreadHandlers}).
     * Cobre: rota do canal UI, e qualquer handler que injete parâmetro {@code @UI}.
     */
    private void ensureUiThreadRule(RouteHandler handler, RouteType type, String rota) {
        if (!fxThreadHandlers.contains(handler)) return;

        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(String.format(
                    "A rota [%s] '%s' manipula componentes visuais e DEVE rodar na " +
                            "JavaFX Application Thread. Use Platform.runLater(...) " +
                            "ou ApiDispatcher.onFxThread(...).",
                    type, rota));
        }
    }

    /**
     * Rota inexistente. Retorna erro no envelope (comportamento mantido por
     * compatibilidade). Nota: alternativamente pode lançar exceção — rota
     * inexistente é typo, e typo é bug de programação, não erro de negócio.
     */
    private ResponseData missing(RouteType type, String rota) {
        return ResponseData.error("Rota inexistente [" + type + "]: " + canonicalize(rota));
    }

    /**
     * ÚNICO ponto de reflexão da cadeia. O cast para ResponseData vive aqui —
     * e nunca mais na view. Um handler que faz {@code return null} falha aqui
     * com a assinatura do método na mensagem, em vez de entregar null à view.
     */
    private ResponseData invoke(RouteHandler handler, Object[] args, RouteType type, String rota) {
        try {
            var result = (ResponseData) handler.method().invoke(handler.bean(), args);
            if (result == null) {
                throw new IllegalStateException(String.format(
                        "%s.%s retornou null — todo handler deve devolver " +
                                "ResponseData.success() ou ResponseData.error(...).",
                        handler.bean().getClass().getSimpleName(), handler.method().getName()));
            }
            return result;
        } catch (InvocationTargetException e) {
            var cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err) throw err;
            throw new RuntimeException("Falha na rota [" + type + "] '" + rota + "'", cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Falha reflexiva em [" + type + "] '" + rota + "'", e);
        }
    }

    /**
     * Binding via resolvers. Erro de parâmetro lança {@link RouteBindingException}
     * (não-checked, com contexto completo) — propagando sem catch intermediário
     * que pudesse re-rotular uma exceção do controller como erro de binding.
     */
    private Object[] resolveArgs(RouteHandler handler, RouteRequest request) {
        var method = handler.method();
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
                throw new RouteBindingException(String.format(
                        "%s.%s: parâmetro '%s' (%s) sem anotação suportada — " +
                                "use @Payload, @RouteVar ou @UI.",
                        handler.bean().getClass().getSimpleName(), method.getName(),
                        param.getName(), param.getType().getSimpleName()));
            }
        }
        return args;
    }

    // =====================================================================
    // AÇÃO AD-HOC (endurecida)
    // =====================================================================

    /**
     * Invoca um método SEM anotação de rota, localizado pelo bean de qualquer
     * canal. Restrições aplicadas para não furar o modelo de segurança:
     * <ul>
     *   <li>respeita a regra da FX Thread (se o bean alvo exige);</li>
     *   <li>o método alvo DEVE retornar ResponseData (mesmo contrato dos handlers);</li>
     *   <li>{@code return null} também é rejeitado.</li>
     * </ul>
     */
    public ResponseData dispatchAction(String rota, String actionName) {
        Objects.requireNonNull(actionName);

        var located = locateAnyChannel(rota);
        if (located == null) return ResponseData.error("Rota inexistente: " + canonicalize(rota));

        var channel = located.getKey();
        var handler = located.getValue();

        ensureUiThreadRule(handler, channel, rota);

        Method target = null;
        for (Class<?> c = handler.bean().getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try { target = c.getDeclaredMethod(actionName); break; }
            catch (NoSuchMethodException ignored) {}
        }
        if (target == null) {
            return ResponseData.error("Método '" + actionName + "' não encontrado em: " + rota);
        }
        if (!ResponseData.class.isAssignableFrom(target.getReturnType())) {
            throw new IllegalStateException(String.format(
                    "%s.%s (ação ad-hoc) deve retornar ResponseData — retorno atual: %s.",
                    handler.bean().getClass().getSimpleName(), actionName,
                    target.getReturnType().getSimpleName()));
        }
        try {
            if (!target.canAccess(handler.bean())) target.setAccessible(true);
            var result = (ResponseData) target.invoke(handler.bean());
            if (result == null) {
                throw new IllegalStateException(
                        "Ação '" + actionName + "' retornou null — devolva ResponseData.");
            }
            return result;
        } catch (InvocationTargetException e) {
            var cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err) throw err;
            throw new RuntimeException("Erro na ação '" + actionName + "'", cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Erro reflexivo na ação '" + actionName + "'", e);
        }
    }

    /** Localiza o primeiro registro (qualquer canal) do caminho. */
    private Map.Entry<RouteType, RouteHandler> locateAnyChannel(String rota) {
        var verbMap = routes.get(canonicalize(rota));
        return (verbMap == null || verbMap.isEmpty())
                ? null : verbMap.entrySet().iterator().next();
    }

    // =====================================================================
    // UTILITÁRIOS
    // =====================================================================

    /**
     * Executa na FX Thread (bloqueante). <b>Atenção:</b> chamar a partir de uma
     * thread que segure um lock de que a FX Thread dependa causará deadlock —
     * prefira chamar de threads de fundo "livres" (Task, Service).
     */
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

    /** Uso interno/testes — limpa a tabela de rotas. */
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

    /** Cópia tolerante: aceita null/vazio e não exige imutabilidade da origem. */
    private static Map<String, Object> tolerantCopy(Map<String, Object> source) {
        return (source == null || source.isEmpty()) ? Map.of() : new LinkedHashMap<>(source);
    }
}