// EventBus.java v3.1 - 2026-08-22
// System.Logger, EventLog como Record, Fail-Fast, Java 17+ compatível
package com.ossobo.winterfx.event;

import com.ossobo.winterfx.anotations.PostConstruct;
import com.ossobo.winterfx.anotations.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * EventBus v3.1
 *
 * <p>Responsabilidade: Gerenciar publicação e assinatura de eventos de forma
 * assíncrona e desacoplada.</p>
 *
 * <p>Padrões: Pub/Sub, Observer, DI-ready</p>
 *
 * <p><b>Melhorias v3.1:</b></p>
 * <ul>
 *   <li>✅ System.Logger (sem dependências externas)</li>
 *   <li>✅ EventLog como Record (imutabilidade real)</li>
 *   <li>✅ Fail-Fast: Objects.requireNonNull no publish()</li>
 *   <li>✅ Subscription para prevenção de memory leak</li>
 *   <li>✅ Compatível com Java 17+ (remove(0) em vez de removeFirst())</li>
 * </ul>
 *
 * @version 3.1 (22/08/2026) - Java 17+ compatível
 */
@Service
public class EventBus {

    private static final System.Logger LOGGER = System.getLogger(EventBus.class.getName());

    // Mapa: Tipo do Evento → Lista de Consumers (Thread-safe)
    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    // Mapa para assinaturas únicas (auto-removíveis)
    private final Map<Class<?>, List<Consumer<Object>>> onceListeners = new ConcurrentHashMap<>();

    // Histórico de eventos para debugging (Record imutável)
    private final List<EventLog> eventHistory = new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY = 100;

    // ============================================================
    // INICIALIZAÇÃO
    // ============================================================

    @PostConstruct
    public void init() {
        LOGGER.log(System.Logger.Level.INFO, "EventBus v3.1 inicializado");
    }

    // ============================================================
    // MÉTODOS DE ASSINATURA
    // ============================================================

    /**
     * Registra um listener para um tipo específico de evento.
     *
     * @param eventType Tipo do evento (ex: ConexaoEvent.class)
     * @param listener Consumer que receberá o evento
     * @return Subscription objeto para descadastramento
     * @throws NullPointerException se eventType ou listener for null
     */
    @SuppressWarnings("unchecked")
    public <T> Subscription subscribe(Class<T> eventType, Consumer<T> listener) {
        Objects.requireNonNull(eventType, "eventType não pode ser null");
        Objects.requireNonNull(listener, "listener não pode ser null");

        var list = listeners.computeIfAbsent(
                eventType,
                k -> new CopyOnWriteArrayList<>()
        );

        // Cast seguro do consumer para o formato interno
        Consumer<Object> internalListener = event -> listener.accept((T) event);
        list.add(internalListener);

        LOGGER.log(System.Logger.Level.DEBUG,
                "Assinatura registrada para: {0}", eventType.getSimpleName());

        // Retorna a capacidade de fazer unsubscribe
        return () -> {
            list.remove(internalListener);
            LOGGER.log(System.Logger.Level.DEBUG,
                    "Assinatura removida para: {0}", eventType.getSimpleName());
        };
    }

    /**
     * Registra um listener que será executado apenas uma vez.
     * Após a primeira execução, é automaticamente removido.
     *
     * @param eventType Tipo do evento
     * @param listener Consumer que receberá o evento
     * @return Subscription objeto para descadastramento manual (se necessário)
     * @throws NullPointerException se eventType ou listener for null
     */
    @SuppressWarnings("unchecked")
    public <T> Subscription subscribeOnce(Class<T> eventType, Consumer<T> listener) {
        Objects.requireNonNull(eventType, "eventType não pode ser null");
        Objects.requireNonNull(listener, "listener não pode ser null");

        var list = onceListeners.computeIfAbsent(
                eventType,
                k -> new CopyOnWriteArrayList<>()
        );

        // Truque do array de 1 posição para a lambda referenciar a si mesma
        @SuppressWarnings("unchecked")
        Consumer<Object>[] holder = new Consumer[1];

        holder[0] = event -> {
            listener.accept((T) event);
            // Auto-remove após execução usando a referência correta
            list.remove(holder[0]);
        };

        list.add(holder[0]);
        LOGGER.log(System.Logger.Level.DEBUG,
                "Assinatura única registrada para: {0}", eventType.getSimpleName());

        return () -> list.remove(holder[0]);
    }

    // ============================================================
    // MÉTODO DE PUBLICAÇÃO - FAIL-FAST
    // ============================================================

    /**
     * Publica um evento para todos os assinantes.
     * Executa na thread do chamador.
     *
     * <p><b>Fail-Fast:</b> Lança NullPointerException se event for null.</p>
     *
     * <p><b>NOTA:</b> O WinterFX Scanner cuidará de envolver em Platform.runLater()
     * ou Async automaticamente baseado nas anotações do método receptor.</p>
     *
     * @param event Evento a ser publicado (não pode ser null)
     * @throws NullPointerException se event for null
     */
    public void publish(Object event) {
        // FAIL-FAST: Nunca aceitar evento nulo
        Objects.requireNonNull(event, "Evento não pode ser nulo. Verifique a lógica do publicador.");

        var eventType = event.getClass();
        var startTime = System.currentTimeMillis();

        // 1. Registra no histórico
        logEvent(event);

        // 2. Publica para assinantes normais
        notifyListeners(eventType, event, listeners.get(eventType));

        // 3. Publica para assinantes únicos
        notifyListeners(eventType, event, onceListeners.get(eventType));

        var elapsed = System.currentTimeMillis() - startTime;

        if (elapsed > 100) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Publicação de {0} demorou {1}ms",
                    eventType.getSimpleName(), elapsed);
        }

        LOGGER.log(System.Logger.Level.DEBUG,
                "Evento publicado: {0} ({1}ms)",
                eventType.getSimpleName(), elapsed);
    }

    // ============================================================
    // MÉTODOS PRIVADOS
    // ============================================================

    private void notifyListeners(Class<?> eventType, Object event, List<Consumer<Object>> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        for (var listener : list) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                // Captura para não quebrar a publicação para os próximos listeners
                LOGGER.log(System.Logger.Level.ERROR,
                        "Erro no listener de {0}: {1}",
                        eventType.getSimpleName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Registra o evento no histórico.
     *
     * <p><b>Java 17+ compatível:</b> Usa remove(0) em vez de removeFirst()
     * para compatibilidade com Java 17 (SequencedCollection é Java 21+).</p>
     */
    private void logEvent(Object event) {
        if (eventHistory.size() >= MAX_HISTORY) {
            // remove(0) é compatível com Java 17+
            // removeFirst() só existe a partir do Java 21 (SequencedCollection)
            eventHistory.remove(0);
        }
        eventHistory.add(new EventLog(event, System.currentTimeMillis()));
    }

    // ============================================================
    // MÉTODOS DE CONSULTA E UTILIDADE
    // ============================================================

    /**
     * Verifica se há assinantes para um tipo de evento.
     */
    public boolean hasSubscribers(Class<?> eventType) {
        Objects.requireNonNull(eventType, "eventType não pode ser null");
        var list = listeners.get(eventType);
        return list != null && !list.isEmpty();
    }

    /**
     * Retorna a quantidade de assinantes para um tipo de evento.
     */
    public int getSubscriberCount(Class<?> eventType) {
        Objects.requireNonNull(eventType, "eventType não pode ser null");
        var list = listeners.get(eventType);
        return list != null ? list.size() : 0;
    }

    /**
     * Limpa todos os listeners e o histórico.
     */
    public void clear() {
        listeners.clear();
        onceListeners.clear();
        eventHistory.clear();
        LOGGER.log(System.Logger.Level.INFO, "EventBus limpo");
    }

    /**
     * Retorna uma cópia imutável do histórico de eventos.
     */
    public List<EventLog> getEventHistory() {
        return List.copyOf(eventHistory);
    }

    // ============================================================
    // RECORD: EventLog (imutável)
    // ============================================================

    /**
     * Record imutável para logging de eventos.
     *
     * <p>Substitui a classe interna anterior, eliminando boilerplate
     * e garantindo imutabilidade real em nível de linguagem.</p>
     */
    public record EventLog(Object event, long timestamp) {
        /**
         * Retorna o nome simples da classe do evento.
         */
        public String getType() {
            return event != null ? event.getClass().getSimpleName() : "null";
        }

        @Override
        public String toString() {
            return String.format("[%s] %d", getType(), timestamp);
        }
    }
}