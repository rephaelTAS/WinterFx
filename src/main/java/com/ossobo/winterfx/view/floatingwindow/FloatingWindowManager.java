// FloatingWindowManager.java v10.0 - 2026-08-24
// LAZY: injeta apenas o wrapper, view carregada apenas no show()
// DESACOPLADO: StageManager faz o loading, FXMLService faz o binding
package com.ossobo.winterfx.view.floatingwindow;

import com.ossobo.winterfx.resources.ResourceModule;
import com.ossobo.winterfx.resources.enums.Modality;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.floatingwindow.anotations.FloatingWindow;

import javafx.stage.Stage;
import javafx.stage.Window;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.System.Logger;

/**
 * 🪟 FloatingWindowManager v10.0 - LAZY
 *
 * <p><b>Princípio:</b> O FloatingWindowManager é o PALCO.</p>
 * <p>Ele configura a janela (tamanho, modal, undecorated, etc.) mas NÃO carrega a view.</p>
 * <p>A view só é carregada quando {@link StageForFloatingWindow#show()} é chamado.</p>
 *
 * <p><b>Fluxo:</b></p>
 * <ol>
 *   <li>@FloatingWindow é processado → injeta StageForFloatingWindow (LAZY)</li>
 *   <li>Usuário chama formfuncionario.show()</li>
 *   <li>StageForFloatingWindow → StageManager.loadFloatingView()</li>
 *   <li>StageManager → FXMLService.load() → rebindButtons()</li>
 * </ol>
 *
 * @version 10.0 (24/08/2026) - LAZY + Desacoplamento
 */
public class FloatingWindowManager {

    private static final Logger LOGGER = System.getLogger(FloatingWindowManager.class.getName());

    private final ResourceModule resourceModule;
    private final StageManager stageManager;

    private final Map<String, Stage> managedWindows = new ConcurrentHashMap<>();
    private final Deque<Stage> modalStack = new ArrayDeque<>();
    private int instanceCounter = 0;

    // ============================================================
    // CONSTRUTOR
    // ============================================================

    public FloatingWindowManager(ResourceModule resourceModule, StageManager stageManager) {
        this.resourceModule = Objects.requireNonNull(resourceModule);
        this.stageManager = Objects.requireNonNull(stageManager);
    }

    // ============================================================
    // PROCESSAMENTO DE ANOTAÇÕES
    // ============================================================

    public void processAnnotations(Object bean) {
        if (bean == null) return;
        Class<?> clazz = bean.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            FloatingWindow ann = field.getAnnotation(FloatingWindow.class);
            if (ann != null) {
                processFloatingWindow(bean, field, ann);
            }
        }
    }

    // ============================================================
    // PROCESSAMENTO PRINCIPAL - INJEÇÃO LAZY
    // ============================================================

    /**
     * Processa a anotação @FloatingWindow - INJEÇÃO LAZY.
     *
     * <p>NÃO carrega a view agora. Apenas valida que a view existe
     * e injeta um {@link StageForFloatingWindow} que carregará a view
     * quando {@code show()} for chamado.</p>
     */
    private void processFloatingWindow(Object bean, Field field, FloatingWindow annotation) {
        String viewId = annotation.viewId();

        try {
            // ✅ APENAS VALIDA que a view existe (Fail-Fast no bootstrap)
            resourceModule.requireView(viewId);

            // ✅ CRIA O WRAPPER LAZY (NÃO carrega a view agora)
            var window = new StageForFloatingWindow(
                    stageManager,
                    viewId,
                    annotation.singleton(),
                    annotation.title(),
                    annotation.width(),
                    annotation.height(),
                    annotation.resizable(),
                    annotation.alwaysOnTop(),
                    true,
                    annotation.autoClose(),
                    annotation.autoOpen(),
                    annotation.modality(),
                    annotation.owner(),
                    this  // Referência ao FloatingWindowManager para gerenciar a janela
            );

            // ✅ INJETA O WRAPPER no campo
            field.setAccessible(true);
            field.set(bean, window);

            LOGGER.log(Logger.Level.INFO, "🪟 @FloatingWindow LAZY: {0} → {1} [aguardando show()]",
                    viewId, field.getName());

        } catch (Exception e) {
            LOGGER.log(Logger.Level.ERROR, "❌ Erro ao processar @FloatingWindow: " + viewId, e);
            // Não lança exceção para não quebrar o bootstrap
        }
    }

    // ============================================================
    // MÉTODOS CHAMADOS PELO StageForFloatingWindow
    // ============================================================

    /**
     * Registra uma janela no gerenciamento.
     * Chamado pelo StageForFloatingWindow quando a janela é criada.
     */
    public void registerWindow(String key, Stage stage) {
        managedWindows.put(key, stage);
        LOGGER.log(Logger.Level.DEBUG, "Janela registrada: {0}", key);
    }

    /**
     * Remove uma janela do gerenciamento.
     * Chamado pelo StageForFloatingWindow quando a janela é fechada.
     */
    public void unregisterWindow(String key) {
        managedWindows.remove(key);
        LOGGER.log(Logger.Level.DEBUG, "Janela removida: {0}", key);
    }

    /**
     * Adiciona uma janela à pilha modal.
     */
    public void pushModalStack(Stage stage) {
        modalStack.push(stage);
    }

    /**
     * Remove uma janela da pilha modal.
     */
    public void popModalStack(Stage stage) {
        modalStack.remove(stage);
    }

    /**
     * Resolve o owner da janela.
     */
    public Window resolveOwner(String ownerId) {
        // 1. Owner explícito por anotação
        if (ownerId != null && !ownerId.isEmpty()) {
            Stage stage = managedWindows.get(ownerId);
            if (stage != null && stage.isShowing()) return stage;
        }

        // 2. Cadeia modal: última janela modal na pilha
        if (!modalStack.isEmpty()) {
            Stage top = modalStack.peek();
            if (top != null && top.isShowing()) return top;
        }

        // 3. Fallback: janela ativa
        return Stage.getWindows().stream()
                .filter(w -> w instanceof Stage && w.isShowing())
                .findFirst()
                .orElse(null);
    }

    /**
     * Converte Modality enum para JavaFX Modality.
     */
    public javafx.stage.Modality convertModality(Modality m) {
        return switch (m) {
            case APPLICATION_MODAL -> javafx.stage.Modality.APPLICATION_MODAL;
            case WINDOW_MODAL -> javafx.stage.Modality.WINDOW_MODAL;
            case NONE -> javafx.stage.Modality.NONE;
            default -> javafx.stage.Modality.NONE;
        };
    }

    /**
     * Gera uma chave única para janelas não-singleton.
     */
    public String generateKey(String viewId) {
        return viewId + "-" + (++instanceCounter);
    }

    // ============================================================
    // API PÚBLICA
    // ============================================================

    public void abrir(String viewId) {
        Stage stage = managedWindows.get(viewId);
        if (stage != null) {
            if (stage.isShowing()) stage.toFront();
            else stage.show();
        }
    }

    public void fechar(String viewId) {
        Stage stage = managedWindows.remove(viewId);
        if (stage != null) stage.close();
    }

    public void fecharTodas() {
        managedWindows.values().forEach(Stage::close);
        managedWindows.clear();
        modalStack.clear();
        LOGGER.log(Logger.Level.INFO, "🪟 Todas as janelas fechadas");
    }

    public Stage getWindow(String viewId) {
        return managedWindows.get(viewId);
    }

    public StageManager getStageManager() {
        return stageManager;
    }

    // ============================================================
    // DIAGNÓSTICO
    // ============================================================

    public void diagnostic() {
        LOGGER.log(Logger.Level.INFO, "=== 🪟 FLOATING WINDOW MANAGER DIAGNÓSTICO ===");
        LOGGER.log(Logger.Level.INFO, "Total de janelas: {0}", managedWindows.size());
        LOGGER.log(Logger.Level.INFO, "Modal stack: {0}", modalStack.size());
        LOGGER.log(Logger.Level.INFO, "Janelas abertas:");
        for (Map.Entry<String, Stage> entry : managedWindows.entrySet()) {
            String status = entry.getValue().isShowing() ? "🟢 ABERTA" : "🔴 FECHADA";
            LOGGER.log(Logger.Level.INFO, "  {0} → {1}", entry.getKey(), status);
        }
        if (!modalStack.isEmpty()) {
            LOGGER.log(Logger.Level.INFO, "Cadeia modal:");
            for (Stage s : modalStack) {
                LOGGER.log(Logger.Level.INFO, "  → {0}", s.getTitle());
            }
        }
        LOGGER.log(Logger.Level.INFO, "================================================");
    }
}