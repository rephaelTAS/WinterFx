package com.ossobo.winterfx.core;

import com.ossobo.winterfx.di.annotations.FloatingWindow;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor.Modality;
import com.ossobo.winterfx.resources.registry.ResourceRegistry;
import com.ossobo.winterfx.view.loader.LoadedView;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🪟 FloatingWindowManager v2.0
 *
 * Processa anotações @FloatingWindow em campos.
 * Integrado ao sistema de injeção automática.
 *
 * <p>O USUÁRIO SÓ PRECISA DAS ANOTAÇÕES:</p>
 * <pre>
 * {@code
 * @FloatingWindow(value = "detalhes-usuario", modality = Modality.WINDOW_MODAL)
 * private Stage janelaDetalhes;        // Stage aparece aqui SOZINHO!
 *
 * @GetController("detalhes-usuario")
 * private DetalhesUsuarioController ctrl;  // Controller aparece aqui SOZINHO!
 * }
 * </pre>
 */
public class FloatingWindowManager {

    private final ResourceRegistry registry;
    private final StageManager stageManager;

    /** Janelas gerenciadas: viewId -> Stage */
    private final Map<String, Stage> managedWindows = new ConcurrentHashMap<>();

    public FloatingWindowManager(ResourceRegistry registry, StageManager stageManager) {
        this.registry = registry;
        this.stageManager = stageManager;
    }

    // =============================================
    // 🔥 PROCESSAMENTO DE ANOTAÇÕES
    // =============================================

    /**
     * Processa @FloatingWindow em um bean.
     * Cria e configura os Stages automaticamente.
     */
    public void processAnnotations(Object bean) {
        if (bean == null) return;

        Class<?> clazz = bean.getClass();

        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            FloatingWindow annotation = field.getAnnotation(FloatingWindow.class);
            if (annotation != null) {
                processFloatingWindow(bean, field, annotation);
            }
        }
    }

    /**
     * Processa uma anotação @FloatingWindow.
     */
    private void processFloatingWindow(Object bean, java.lang.reflect.Field field,
                                       FloatingWindow annotation) {
        String viewId = annotation.value();

        try {
            // Busca o ViewDescriptor
            ViewDescriptor descriptor = registry.findViewById(viewId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "View não registrada: '" + viewId + "'"));

            // Carrega a view
            LoadedView<?> loadedView = stageManager.loadView(viewId);

            // Cria o Stage
            Stage stage = new Stage();
            stage.setTitle(!annotation.title().isEmpty() ? annotation.title() : descriptor.getTitle());
            stage.initModality(convertModality(annotation.modality()));

            Scene scene = new Scene(loadedView.getRoot(), descriptor.getWidth(), descriptor.getHeight());
            stage.setScene(scene);

            stage.setResizable(descriptor.isResizable());

            if (descriptor.isCentered()) {
                stage.centerOnScreen();
            }

            // Singleton: fecha janela anterior se existir
            if (annotation.singleton()) {
                Stage existing = managedWindows.get(viewId);
                if (existing != null && existing.isShowing()) {
                    existing.close();
                }
            }

            // Registra a janela
            managedWindows.put(viewId, stage);

            // Cleanup ao fechar
            stage.setOnHidden(e -> {
                if (annotation.singleton()) {
                    managedWindows.remove(viewId);
                }
            });

            // Injeta o Stage no campo
            field.setAccessible(true);
            field.set(bean, stage);

            System.out.println("🪟 Janela flutuante configurada: '" + viewId +
                    "' [modality=" + annotation.modality() +
                    ", singleton=" + annotation.singleton() +
                    ", lazy=" + annotation.lazy() + "]");

            // Auto-open: abre automaticamente
            if (annotation.autoOpen()) {
                stage.show();
                System.out.println("🪟 Janela flutuante aberta automaticamente: '" + viewId + "'");
            }

        } catch (Exception e) {
            System.err.println("❌ Erro @FloatingWindow '" + viewId +
                    "': " + e.getMessage());
            throw new RuntimeException("Falha ao configurar janela flutuante: " + viewId, e);
        }
    }

    // =============================================
    // API DE CONVENIÊNCIA
    // =============================================

    /**
     * Abre uma janela gerenciada.
     */
    public void abrir(String viewId) {
        Stage stage = managedWindows.get(viewId);
        if (stage != null) {
            if (stage.isShowing()) {
                stage.toFront();
            } else {
                stage.show();
            }
        }
    }

    /**
     * Fecha uma janela gerenciada.
     */
    public void fechar(String viewId) {
        Stage stage = managedWindows.get(viewId);
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Fecha todas as janelas.
     */
    public void fecharTodas() {
        managedWindows.values().forEach(Stage::close);
        managedWindows.clear();
    }

    private javafx.stage.Modality convertModality(Modality modality) {
        return switch (modality) {
            case APPLICATION_MODAL -> javafx.stage.Modality.APPLICATION_MODAL;
            case WINDOW_MODAL -> javafx.stage.Modality.WINDOW_MODAL;
            case NONE -> javafx.stage.Modality.NONE;
        };
    }
}