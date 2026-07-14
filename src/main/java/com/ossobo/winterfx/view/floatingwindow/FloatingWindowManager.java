// FloatingWindowManager.java v8.0 - 2026-07-03
// Com modalStack para cadeia modal (abrir janelas uma dentro da outra)
package com.ossobo.winterfx.view.floatingwindow;

import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.Modality;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.floatingwindow.anotations.FloatingWindow;
import com.ossobo.winterfx.view.loader.LoadedView;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 🪟 FloatingWindowManager v8.0
 *
 * <p>Suporte completo a janelas flutuantes com cadeia modal.</p>
 *
 * <p><b>Recursos:</b></p>
 * <ul>
 *   <li><b>singleton = true:</b> Reutiliza o mesmo Stage (cache)</li>
 *   <li><b>singleton = false:</b> Cria NOVO Stage a cada chamada</li>
 *   <li><b>modalStack:</b> Pilha de janelas modais para cadeia A → B → C</li>
 *   <li><b>autoClose:</b> Fecha ao perder foco</li>
 *   <li><b>resolveOwner:</b> Owner por anotação, cadeia modal ou janela ativa</li>
 * </ul>
 *
 * @version 8.0 (03/07/2026)
 */
public class FloatingWindowManager {

    private static final Logger LOGGER = Logger.getLogger(FloatingWindowManager.class.getName());

    private final ResourceRegistry registry;
    private final StageManager stageManager;

    private final Map<String, Stage> managedWindows = new ConcurrentHashMap<>();
    private final Deque<Stage> modalStack = new ArrayDeque<>();
    private int instanceCounter = 0;

    // ============================================================
    // CONSTRUTOR
    // ============================================================

    public FloatingWindowManager(ResourceRegistry registry, StageManager stageManager) {
        this.registry = registry;
        this.stageManager = stageManager;
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
    // PROCESSAMENTO PRINCIPAL
    // ============================================================

    private void processFloatingWindow(Object bean, Field field, FloatingWindow annotation) {
        String viewId = annotation.viewId();
        boolean singleton = annotation.singleton();

        try {
            ViewDescriptor descriptor = registry.findViewById(viewId)
                    .orElseThrow(() -> new IllegalArgumentException("View não registrada: '" + viewId + "'"));

            // ============================================================
            // SINGLETON: reutiliza Stage existente
            // ============================================================

            if (singleton) {
                Stage existing = managedWindows.get(viewId);
                if (existing != null && existing.isShowing()) {
                    existing.toFront();
                    field.setAccessible(true);
                    field.set(bean, existing);
                    return;
                }
            }

            // ============================================================
            // CARREGA A VIEW
            // ============================================================

            LoadedView<?> loadedView = stageManager.loadFloatingView(viewId, singleton);

            // ============================================================
            // CRIA O STAGE
            // ============================================================

            Stage stage = new Stage();
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.setTitle(!annotation.title().isEmpty() ? annotation.title() : descriptor.getTitle());

            javafx.stage.Modality modality = convertModality(annotation.modality());
            stage.initModality(modality);

            Window owner = resolveOwner(annotation.owner());
            if (owner != null && owner != stage) {
                stage.initOwner(owner);
            }

            Scene scene = new Scene(
                    loadedView.getRoot(),
                    annotation.width() > 0 ? annotation.width() : descriptor.getWidth(),
                    annotation.height() > 0 ? annotation.height() : descriptor.getHeight()
            );
            stage.setScene(scene);
            stage.setResizable(annotation.resizable());
            stage.setAlwaysOnTop(annotation.alwaysOnTop());

            if (descriptor.isCentered()) {
                stage.centerOnScreen();
            }

            if (annotation.autoClose()) {
                stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal && stage.isShowing()) {
                        stage.close();
                    }
                });
            }

            // ============================================================
            // MODAL STACK — cadeia modal (A → B → C)
            // ============================================================

            if (modality != javafx.stage.Modality.NONE) {
                stage.setOnShown(e -> modalStack.push(stage));
            }
            stage.setOnHidden(e -> {
                modalStack.remove(stage);
                if (singleton) {
                    managedWindows.remove(viewId);
                }
            });

            // ============================================================
            // REGISTRA E INJETA
            // ============================================================

            if (singleton) {
                managedWindows.put(viewId, stage);
            } else {
                String stageKey = viewId + "-" + (++instanceCounter);
                managedWindows.put(stageKey, stage);
            }

            field.setAccessible(true);
            field.set(bean, stage);

            if (annotation.autoOpen()) {
                stage.show();
            }

            String mode = singleton ? "SINGLETON" : "MÚLTIPLA";
            LOGGER.info(() -> "🪟 @FloatingWindow: " + viewId +
                    " → " + field.getName() +
                    " [" + mode + "]" +
                    " (instances: " + managedWindows.size() + ")" +
                    " (modalStack: " + modalStack.size() + ")");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Erro ao processar @FloatingWindow: " + viewId, e);
        }
    }

    // ============================================================
    // UTILIDADES
    // ============================================================

    private javafx.stage.Modality convertModality(Modality m) {
        return switch (m) {
            case APPLICATION_MODAL -> javafx.stage.Modality.APPLICATION_MODAL;
            case WINDOW_MODAL -> javafx.stage.Modality.WINDOW_MODAL;
            case NONE -> javafx.stage.Modality.NONE;
            default -> javafx.stage.Modality.NONE;
        };
    }

    private Window resolveOwner(String ownerId) {
        // 1. Owner explícito por anotação
        if (ownerId != null && !ownerId.isEmpty()) {
            Stage stage = managedWindows.get(ownerId);
            if (stage != null && stage.isShowing()) return stage;
        }

        // 2. Cadeia modal: última janela modal na pilha
        if (!modalStack.isEmpty()) {
            Stage top = modalStack.peek();
            if (top.isShowing()) return top;
        }

        // 3. Fallback: janela ativa
        return Stage.getWindows().stream()
                .filter(w -> w instanceof Stage && w.isShowing())
                .findFirst()
                .orElse(null);
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
        LOGGER.info("🪟 Todas as janelas fechadas");
    }

    public Stage getWindow(String viewId) {
        return managedWindows.get(viewId);
    }

    // ============================================================
    // DIAGNÓSTICO
    // ============================================================

    public void diagnostic() {
        System.out.println("=== 🪟 FLOATING WINDOW MANAGER DIAGNÓSTICO ===");
        System.out.println("Total de janelas: " + managedWindows.size());
        System.out.println("Modal stack: " + modalStack.size());
        System.out.println("Janelas abertas:");
        for (Map.Entry<String, Stage> entry : managedWindows.entrySet()) {
            String status = entry.getValue().isShowing() ? "🟢 ABERTA" : "🔴 FECHADA";
            System.out.println("  " + entry.getKey() + " → " + status);
        }
        if (!modalStack.isEmpty()) {
            System.out.println("Cadeia modal:");
            for (Stage s : modalStack) {
                System.out.println("  → " + s.getTitle());
            }
        }
        System.out.println("================================================");
    }
}