package com.ossobo.winterfx.event;

import com.ossobo.winterfx.anotations.PostConstruct;
import com.ossobo.winterfx.anotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * EventBus v2.0
 *
 * Responsabilidade: Gerenciar publicação e assinatura de eventos de forma assíncrona e desacoplada.
 * Padrões: Pub/Sub, Observer, Singleton via DI
 *
 * Atualizações v2.0:
 * - Bug fixado no subscribeOnce (auto-referência de lambda).
 * - Retorna Subscription para gerenciamento de ciclo de vida (prevenção de memory leak).
 * - Otimizado para integração com @EventListener via reflection.
 */
@Service
public class EventBus {

    private static final Logger logger = LoggerFactory.getLogger(EventBus.class);

    // Mapa: Tipo do Evento → Lista de Consumers (Thread-safe)
    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    // Mapa para assinaturas únicas (auto-removíveis)
    private final Map<Class<?>, List<Consumer<Object>>> onceListeners = new ConcurrentHashMap<>();

    // Histórico de eventos para debugging
    private final List<EventLog> eventHistory = new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY = 100;

    @PostConstruct
    public void init() {
        logger.info("📡 EventBus v2.0 inicializado");
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
     */
    @SuppressWarnings("unchecked")
    public <T> Subscription subscribe(Class<T> eventType, Consumer<T> listener) {
        List<Consumer<Object>> list = listeners.computeIfAbsent(
                eventType,
                k -> new CopyOnWriteArrayList<>()
        );

        // Cast seguro do consumer para o formato interno
        Consumer<Object> internalListener = event -> listener.accept((T) event);
        list.add(internalListener);

        logger.debug("📥 Assinatura registrada para: {}", eventType.getSimpleName());

        // Retorna a capacidade de fazer unsubscribe
        return () -> {
            list.remove(internalListener);
            logger.debug("📤 Assinatura removida para: {}", eventType.getSimpleName());
        };
    }

    /**
     * Registra um listener que será executado apenas uma vez.
     * Após a primeira execução, é automaticamente removido.
     *
     * @param eventType Tipo do evento
     * @param listener Consumer que receberá o evento
     * @return Subscription objeto para descadastramento manual (se necessário)
     */
    @SuppressWarnings("unchecked")
    public <T> Subscription subscribeOnce(Class<T> eventType, Consumer<T> listener) {
        List<Consumer<Object>> list = onceListeners.computeIfAbsent(
                eventType,
                k -> new CopyOnWriteArrayList<>()
        );

        // Truque do array de 1 posição para a lambda referenciar a si mesma
        final Consumer<Object>[] holder = new Consumer[1];

        holder[0] = event -> {
            listener.accept((T) event);
            // Auto-remove após execução usando a referência correta
            list.remove(holder[0]);
        };

        list.add(holder[0]);
        logger.debug("📥 Assinatura única registrada para: {}", eventType.getSimpleName());

        return () -> list.remove(holder[0]);
    }

    // ============================================================
    // MÉTODO DE PUBLICAÇÃO
    // ============================================================

    /**
     * Publica um evento para todos os assinantes.
     * Executa na thread do chamador.
     * NOTA: O WinterFX Scanner cuidará de envolver em Platform.runLater()
     * ou Async automaticamente baseado nas anotações do método receptor.
     *
     * @param event Evento a ser publicado
     */
    public void publish(Object event) {
        if (event == null) {
            logger.warn("⚠️ Tentativa de publicar evento nulo");
            return;
        }

        Class<?> eventType = event.getClass();
        long startTime = System.currentTimeMillis();

        // 1. Registra no histórico
        logEvent(event);

        // 2. Publica para assinantes normais
        notifyListeners(eventType, event, listeners.get(eventType));

        // 3. Publica para assinantes únicos
        notifyListeners(eventType, event, onceListeners.get(eventType));

        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed > 100) {
            logger.warn("⏱️ Publicação de {} demorou {}ms",
                    eventType.getSimpleName(), elapsed);
        }

        logger.debug("📤 Evento publicado: {} ({}ms)",
                eventType.getSimpleName(), elapsed);
    }

    // ============================================================
    // MÉTODOS PRIVADOS E DE UTILIDADE
    // ============================================================

    private void notifyListeners(Class<?> eventType, Object event, List<Consumer<Object>> list) {
        if (list == null || list.isEmpty()) return;

        for (Consumer<Object> listener : list) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                // Captura para não quebrar a publicação para os próximos listeners
                logger.error("❌ Erro no listener de {}: {}",
                        eventType.getSimpleName(), e.getMessage(), e);
            }
        }
    }

    public boolean hasSubscribers(Class<?> eventType) {
        List<Consumer<Object>> list = listeners.get(eventType);
        return list != null && !list.isEmpty();
    }

    public int getSubscriberCount(Class<?> eventType) {
        List<Consumer<Object>> list = listeners.get(eventType);
        return list != null ? list.size() : 0;
    }

    public void clear() {
        listeners.clear();
        onceListeners.clear();
        eventHistory.clear();
        logger.info("🧹 EventBus limpo");
    }

    public List<EventLog> getEventHistory() {
        return new CopyOnWriteArrayList<>(eventHistory);
    }

    private void logEvent(Object event) {
        if (eventHistory.size() >= MAX_HISTORY) {
            eventHistory.remove(0);
        }
        eventHistory.add(new EventLog(event, System.currentTimeMillis()));
    }

    // ============================================================
    // CLASSE INTERNA DE LOG
    // ============================================================

    public static class EventLog {
        private final Object event;
        private final long timestamp;
        private final String type;

        EventLog(Object event, long timestamp) {
            this.event = event;
            this.timestamp = timestamp;
            this.type = event.getClass().getSimpleName();
        }

        public Object getEvent() { return event; }
        public long getTimestamp() { return timestamp; }
        public String getType() { return type; }

        @Override
        public String toString() {
            return String.format("[%s] %s", type, timestamp);
        }
    }
}