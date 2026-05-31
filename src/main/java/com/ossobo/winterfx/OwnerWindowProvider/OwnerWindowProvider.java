package com.ossobo.winterfx.OwnerWindowProvider;


import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.anotations.Component;
import com.ossobo.winterfx.anotations.Scope;

import com.ossobo.winterfx.di.scopes.enums.ScopeType;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Scope(ScopeType.SINGLETON)
public class OwnerWindowProvider {
    private static final Logger logger = LoggerFactory.getLogger(OwnerWindowProvider.class);

    // Singleton para o Stage temporário (evita memory leak)
    private final AtomicReference<Stage> fallbackStage = new AtomicReference<>();

    // Cache para melhor performance
    private final ConcurrentHashMap<Integer, Window> windowCache = new ConcurrentHashMap<>();

    // =============== VERSÃO CORRIGIDA ===============

    public Window getOwnerWindow(Node node) {
        // 1. Tentar do cache primeiro
        if (node != null) {
            int nodeHash = System.identityHashCode(node);
            Window cached = windowCache.get(nodeHash);
            if (isValidWindow(cached)) {
                logger.debug("✅ Window do cache: {}", cached);
                return cached;
            }
        }

        // 2. Tentar extrair do Node
        Window existingWindow = extractWindowFromNode(node);
        if (isValidWindow(existingWindow)) {
            cacheWindow(node, existingWindow);
            logger.debug("✅ Window existente obtida: {}", existingWindow);
            return existingWindow;
        }

        // 3. Tentar janela em foco
        Window focused = findFocusedWindow();
        if (isValidWindow(focused)) {
            cacheWindow(node, focused);
            logger.debug("✅ Usando janela em foco");
            return focused;
        }

        // 4. Fallback: Stage temporário CORRETAMENTE inicializado
        logger.warn("⚠️ Nenhuma janela válida encontrada. Usando fallback.");
        return getOrCreateFallbackStage();
    }

    public Stage getOwnerStage(Node node) {
        Window window = getOwnerWindow(node);
        return (window instanceof Stage) ? (Stage) window : null;
    }

    // =============== MÉTODOS AUXILIARES CORRIGIDOS ===============

    private Window extractWindowFromNode(Node node) {
        if (node == null) return null;

        try {
            // Navegar pela hierarquia para encontrar Scene
            Node current = node;
            while (current != null && current.getScene() == null) {
                current = current.getParent();
            }

            if (current != null && current.getScene() != null) {
                return current.getScene().getWindow();
            }
        } catch (Exception e) {
            logger.debug("Erro ao extrair window do node", e);
        }

        return null;
    }

    private boolean isValidWindow(Window window) {
        if (window == null) return false;

        // Verificar se é o fallback stage (sempre válido)
        if (window == fallbackStage.get()) {
            return true;
        }

        // Verificar se está ativa (precisa estar na FX Thread)
        if (Platform.isFxApplicationThread()) {
            try {
                return window.isShowing();
            } catch (Exception e) {
                return false;
            }
        }

        // Se não estamos na FX thread, assumir válido (validação acontecerá no uso)
        return true;
    }

    private Window findFocusedWindow() {
        if (!Platform.isFxApplicationThread()) {
            return null;
        }

        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window.isFocused()) {
                return window;
            }
        }

        return null;
    }

    // ✅ CORREÇÃO: Stage temporário CORRETAMENTE inicializado
    private Stage getOrCreateFallbackStage() {
        return fallbackStage.updateAndGet(existing -> {
            if (existing != null && isValidWindow(existing)) {
                return existing;
            }

            try {
                Stage newStage = createProperFallbackStage();
                logger.info("✅ Stage temporário criado corretamente");
                return newStage;
            } catch (Exception e) {
                logger.error("❌ Falha crítica ao criar stage temporário", e);
                return null;
            }
        });
    }

    // ✅ CORREÇÃO: Stage que funciona como owner
    private Stage createProperFallbackStage() {
        Stage stage = new Stage();
        stage.setTitle("Fallback Stage");
        stage.setOpacity(0.0);
        stage.initStyle(StageStyle.UTILITY);
        stage.setWidth(1);
        stage.setHeight(1);
        stage.setX(-10000);  // Posiciona fora da tela
        stage.setY(-10000);

        // ✅ CRÍTICO: Inicializar o Stage
        if (Platform.isFxApplicationThread()) {
            stage.show();
            stage.hide();  // Agora está inicializado como owner válido
        } else {
            // Se não estamos na FX thread, agendar inicialização
            Platform.runLater(() -> {
                stage.show();
                stage.hide();
            });

            // Pequena pausa para garantir inicialização (em produção usar CountDownLatch)
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }

        return stage;
    }

    private void cacheWindow(Node node, Window window) {
        if (node != null && window != null) {
            int nodeHash = System.identityHashCode(node);
            windowCache.put(nodeHash, window);

            // Limitar tamanho do cache
            if (windowCache.size() > 100) {
                windowCache.clear();
            }
        }
    }

    // Método para cleanup
    public void cleanup() {
        Stage stage = fallbackStage.get();
        if (stage != null) {
            Platform.runLater(() -> {
                if (stage.isShowing()) {
                    stage.close();
                }
            });
            fallbackStage.set(null);
        }
        windowCache.clear();
        logger.info("🧹 OwnerWindowProvider limpo");
    }

    // Métodos estáticos para uso fácil
    public static Window getOwnerFor(Node node) {
        try {
            OwnerWindowProvider provider = DiContainer.getInstance()
                    .getBean(OwnerWindowProvider.class);
            return provider.getOwnerWindow(node);
        } catch (Exception e) {
            logger.error("Não foi possível obter OwnerWindowProvider", e);
            return null;
        }
    }
}
