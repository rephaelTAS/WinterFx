package com.ossobo.winterfx.di.reflection;

import com.ossobo.winterfx.anotations.Inject;
import com.ossobo.winterfx.anotations.PostConstruct;
import com.ossobo.winterfx.anotations.PreDestroy;
import com.ossobo.winterfx.scanner.ReflectionScanner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache de alta performance para metadados de reflexão utilizados pelo subsistema de injeção de dependências.
 *
 * <p>Esta classe armazena em memória os resultados das operações de escaneamento reflexivo,
 * evitando a sobrecarga de realizar buscas repetidas por campos, métodos e construtores
 * anotados durante o ciclo de vida da aplicação.</p>
 *
 * <p>O escopo deste cache é restrito às anotações core do módulo de DI:</p>
 * <ul>
 *   <li>{@code @Inject} - Para campos, métodos e construtores.</li>
 *   <li>{@code @PostConstruct} - Para métodos de inicialização.</li>
 *   <li>{@code @PreDestroy} - Para métodos de destruição.</li>
 * </ul>
 *
 * <p>Anotações específicas de outros módulos (como {@code @InjectView}, {@code @InjectImage},
 * {@code @FloatingWindow}) não são armazenadas aqui, sendo de responsabilidade dos
 * {@code DependencyInjector} externos de cada módulo.</p>
 *
 * <p>A implementação é totalmente thread-safe, utilizando estruturas de dados concorrentes
 * e contadores atômicos para o monitoramento de acertos (hits) e falhas (misses) do cache.</p>
 *
 * @version 4.0
 */
public final class ReflectionCache {

    // ===== COMPONENTES =====
    private final Map<Class<?>, Constructor<?>> injectableConstructors = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Field>> injectableFields = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Method>> injectableMethods = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Method>> postConstructMethods = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Method>> preDestroyMethods = new ConcurrentHashMap<>();

    // ===== MÉTRICAS =====
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final ReflectionScanner scanner;

    /**
     * Constrói o cache de reflexão fornecendo o scanner responsável por extrair
     * os metadados brute em caso de ausência no cache.
     *
     * @param scanner O utilitário de escaneamento reflexivo de baixo nível.
     */
    public ReflectionCache(ReflectionScanner scanner) {
        this.scanner = scanner;
    }

    // =============================================
    // CONSTRUTOR INJETÁVEL
    // =============================================

    /**
     * Recupera o construtor adequado para injeção de uma determinada classe.
     *
     * <p>A estratégia de busca segue a seguinte ordem:</p>
     * <ol>
     *     <li>Construtor anotado com {@code @Inject}.</li>
     *     <li>Construtor padrão sem argumentos (público ou não).</li>
     *     <li>Construtor único disponível na classe.</li>
     * </ol>
     *
     * @param type A classe alvo da busca.
     * @return O construtor selecionado para instanciação.
     * @throws RuntimeException Se nenhum construtor adequado for encontrado.
     */
    public Constructor<?> getInjectableConstructor(Class<?> type) {
        return injectableConstructors.computeIfAbsent(type, t -> {
            List<Constructor<?>> ctors = scanner.getConstructors(t);
            for (Constructor<?> c : ctors) {
                if (c.isAnnotationPresent(Inject.class)) return c;
            }
            try {
                return t.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                if (ctors.size() == 1) return ctors.get(0);
                throw new RuntimeException("Nenhum construtor adequado: " + t.getName());
            }
        });
    }

    // =============================================
    // @Inject FIELDS
    // =============================================

    /**
     * Recupera a lista de campos anotados com {@code @Inject} para a classe fornecida.
     *
     * <p>Inclui campos herdados, caso aplicável pelo comportamento do {@link ReflectionScanner}.</p>
     *
     * @param type A classe a ser analisada.
     * @return Uma lista imutável de campos anotados.
     */
    public List<Field> getInjectableFields(Class<?> type) {
        return cache(injectableFields, type,
                () -> scanner.getFieldsWithAnnotation(type, Inject.class));
    }

    // =============================================
    // @Inject METHODS
    // =============================================

    /**
     * Recupera a lista de métodos anotados com {@code @Inject} que possuem pelo menos um parâmetro.
     *
     * <p>Métodos {@code @Inject} sem parâmetros são filtrados, uma vez que a injeção
     * por método exige a resolução de dependências via argumentos.</p>
     *
     * @param type A classe a ser analisada.
     * @return Uma lista filtrada de métodos anotados a serem injetados.
     */
    public List<Method> getInjectableMethods(Class<?> type) {
        return cache(injectableMethods, type, () -> {
            List<Method> methods = scanner.getMethodsWithAnnotation(type, Inject.class);
            return methods.stream().filter(m -> m.getParameterCount() > 0).toList();
        });
    }

    // =============================================
    // @PostConstruct
    // =============================================

    /**
     * Recupera a lista de métodos anotados com {@code @PostConstruct} que não possuem parâmetros.
     *
     * @param type A classe a ser analisada.
     * @return Uma lista filtrada de métodos de pós-construção.
     */
    public List<Method> getPostConstructMethods(Class<?> type) {
        return cache(postConstructMethods, type, () -> {
            List<Method> methods = scanner.getMethodsWithAnnotation(type, PostConstruct.class);
            return methods.stream().filter(m -> m.getParameterCount() == 0).toList();
        });
    }

    // =============================================
    // @PreDestroy
    // =============================================

    /**
     * Recupera a lista de métodos anotados com {@code @PreDestroy} que não possuem parâmetros.
     *
     * @param type A classe a ser analisada.
     * @return Uma lista filtrada de métodos de pré-destruição.
     */
    public List<Method> getPreDestroyMethods(Class<?> type) {
        return cache(preDestroyMethods, type, () -> {
            List<Method> methods = scanner.getMethodsWithAnnotation(type, PreDestroy.class);
            return methods.stream().filter(m -> m.getParameterCount() == 0).toList();
        });
    }

    // =============================================
    // MÉTRICAS
    // =============================================

    /**
     * Retorna o número total de acessos ao cache que resultaram em dados já armazenados.
     *
     * @return O número de acertos (hits).
     */
    public long getHits() { return hits.get(); }

    /**
     * Retorna o número total de acessos ao cache que não encontraram dados prévios,
     * exigindo um novo escaneamento reflexivo.
     *
     * @return O número de falhas (misses).
     */
    public long getMisses() { return misses.get(); }

    /**
     * Retorna um mapa com estatísticas detalhadas de utilização do cache.
     *
     * <p>O mapa contém as chaves: {@code hits}, {@code misses}, {@code total}
     * e {@code hitRatePercent} (taxa de acertos em porcentagem).</p>
     *
     * @return Um mapa ordenado com as estatísticas de performance.
     */
    public Map<String, Long> getStatistics() {
        long h = hits.get();
        long m = misses.get();
        long total = h + m;
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("hits", h);
        stats.put("misses", m);
        stats.put("total", total);
        stats.put("hitRatePercent", total == 0 ? 0 : (h * 100 / total));
        return stats;
    }

    // =============================================
    // LIMPEZA
    // =============================================

    /**
     * Limpa todos os dados armazenados no cache e reseta os contadores de métricas.
     */
    public void clear() {
        injectableConstructors.clear();
        injectableFields.clear();
        injectableMethods.clear();
        postConstructMethods.clear();
        preDestroyMethods.clear();
        hits.set(0);
        misses.set(0);
    }

    // =============================================
    // INTERNO
    // =============================================

    /**
     * Método utilitário central para gerenciar o armazenamento e a contagem de métricas
     * de forma padronizada para todos os mapas de cache.
     *
     * @param <T>     O tipo do dado armazenado.
     * @param map     O mapa de cache específico.
     * @param key     A chave de busca (geralmente a classe alvo).
     * @param loader  Fornecedor do dado caso não exista no cache.
     * @return O dado recuperado do cache ou recém-carregado.
     */
    private <T> T cache(Map<Class<?>, T> map, Class<?> key,
                        java.util.function.Supplier<T> loader) {
        if (map.containsKey(key)) {
            hits.incrementAndGet();
            return map.get(key);
        }
        misses.incrementAndGet();
        T value = loader.get();
        map.put(key, value);
        return value;
    }
}