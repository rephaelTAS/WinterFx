// AlertManager.java v5.0 - 2026-08-22
// Unificação de createStage + suporte a width/height
package com.ossobo.winterfx.view.alert;

import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.AlertType;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.loader.LoadedView;

import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.Objects;

/**
 * AlertManager v5.0 — Gerencia alertas e diálogos modais.
 *
 * <p><b>Princípios aplicados:</b></p>
 * <ul>
 *   <li>✅ DRY: createStage unificado para modal e não-modal</li>
 *   <li>✅ OCP: único ponto de criação de Stage</li>
 *   <li>✅ Declarativo: respeita ViewDescriptor (width, height, centered, etc.)</li>
 *   <li>✅ JavaFX idiomático: PauseTransition</li>
 * </ul>
 *
 * <p>USA {@link StageManager} para carregar FXML.</p>
 *
 * @version 5.0 (22/08/2026) - Unificação de createStage + width/height
 */
public class AlertManager {

    private static final System.Logger LOGGER = System.getLogger(AlertManager.class.getName());

    private final StageManager stageManager;

    public AlertManager(StageManager stageManager) {
        this.stageManager = Objects.requireNonNull(stageManager, "stageManager não pode ser null");
    }

    // ============================================================
    // API PÚBLICA - Alertas Não-Modais
    // ============================================================

    /**
     * Exibe um alerta não-modal com fechamento automático.
     *
     * <p>Configurações extraídas do {@link ViewDescriptor}:</p>
     * <ul>
     *   <li>stageStyle (da anotação @RegisterNotification)</li>
     *   <li>centered (da anotação @RegisterNotification)</li>
     *   <li>alwaysOnTop (da anotação @RegisterNotification)</li>
     *   <li>autoCloseMillis (da anotação @RegisterNotification)</li>
     *   <li>width / height (da anotação @RegisterNotification)</li>
     * </ul>
     */
    public Stage showAlert(String viewId, AlertType type) {
        Objects.requireNonNull(viewId, "viewId não pode ser null");
        Objects.requireNonNull(type, "type não pode ser null");

        var descriptor = stageManager.getDescriptor(viewId);
        var loadedView = stageManager.loadView(viewId);

        var stage = createStage(descriptor, loadedView, false);

        // Prioriza o tempo do descritor, senão usa o padrão do AlertType
        var duration = descriptor.autoCloseMillis() > 0
                ? descriptor.autoCloseMillis()
                : getDuration(type);

        if (duration > 0) {
            scheduleAutoClose(stage, duration);
        }

        stage.show();
        LOGGER.log(System.Logger.Level.DEBUG,
                "Alerta exibido: {0} (tipo: {1}, duração: {2}ms, dimensões: {3}x{4})",
                viewId, type, duration, descriptor.width(), descriptor.height());
        return stage;
    }

    /**
     * Exibe um alerta não-modal com duração personalizada.
     */
    public Stage showAlert(String viewId, long durationMs) {
        Objects.requireNonNull(viewId, "viewId não pode ser null");

        var descriptor = stageManager.getDescriptor(viewId);
        var loadedView = stageManager.loadView(viewId);

        var stage = createStage(descriptor, loadedView, false);

        if (durationMs > 0) {
            scheduleAutoClose(stage, durationMs);
        }

        stage.show();
        LOGGER.log(System.Logger.Level.DEBUG,
                "Alerta exibido: {0} (duração: {1}ms, dimensões: {2}x{3})",
                viewId, durationMs, descriptor.width(), descriptor.height());
        return stage;
    }

    // ============================================================
    // API PÚBLICA - Alertas Modais
    // ============================================================

    /**
     * Exibe um alerta modal bloqueante.
     * Configurações extraídas do {@link ViewDescriptor}.
     */
    public Stage showModal(String viewId) {
        Objects.requireNonNull(viewId, "viewId não pode ser null");

        var descriptor = stageManager.getDescriptor(viewId);
        var loadedView = stageManager.loadView(viewId);

        var stage = createStage(descriptor, loadedView, true);

        stage.showAndWait();
        LOGGER.log(System.Logger.Level.DEBUG,
                "Modal exibido: {0} (dimensões: {1}x{2})",
                viewId, descriptor.width(), descriptor.height());
        return stage;
    }

    /**
     * Exibe um alerta modal bloqueante e retorna true.
     *
     * <p><b>NOTA:</b> Este método sempre retorna true pois não há como saber
     * o resultado do diálogo customizado. Para confirmação com retorno booleano,
     * use o Alert nativo do JavaFX diretamente.</p>
     */
    public boolean showAndWait(String viewId) {
        showModal(viewId);
        return true;
    }

    // ============================================================
    // MÉTODO UNIFICADO DE CRIAÇÃO DE STAGE
    // ============================================================

    /**
     * Cria um Stage respeitando todas as configurações do ViewDescriptor.
     *
     * <p><b>Configurações aplicadas:</b></p>
     * <ul>
     *   <li>stageStyle do descriptor ou UNDECORATED (fallback)</li>
     *   <li>Modality.APPLICATION_MODAL se modal = true</li>
     *   <li>width / height do descriptor (se > 0)</li>
     *   <li>centered do descriptor</li>
     *   <li>alwaysOnTop do descriptor</li>
     * </ul>
     *
     * @param descriptor Descritor com as regras visuais da janela
     * @param loadedView View carregada pelo StageManager
     * @param modal Se true, aplica APPLICATION_MODAL
     * @return Stage configurado
     */
    private Stage createStage(ViewDescriptor descriptor, LoadedView<?> loadedView, boolean modal) {
        var stage = new Stage();

        // ============================================================
        // STAGE STYLE
        // ============================================================
        if (descriptor.stageStyle() != null) {
            stage.initStyle(descriptor.stageStyle().toJavaFX());
        } else {
            stage.initStyle(StageStyle.UNDECORATED); // Fallback seguro
        }

        // ============================================================
        // MODALIDADE
        // ============================================================
        if (modal) {
            stage.initModality(Modality.APPLICATION_MODAL);
        }

        // ============================================================
        // CENA COM DIMENSÕES DO DESCRITOR
        // ============================================================
        var width = descriptor.width();
        var height = descriptor.height();

        if (width > 0 && height > 0) {
            // Usa dimensões explícitas do descritor
            stage.setScene(new Scene(loadedView.root(), width, height));
            LOGGER.log(System.Logger.Level.DEBUG,
                    "Stage com dimensões explícitas: {0}x{1}", width, height);
        } else {
            // Auto-size baseado no conteúdo FXML
            stage.setScene(new Scene(loadedView.root()));
            LOGGER.log(System.Logger.Level.DEBUG,
                    "Stage com auto-size (sem dimensões explícitas)");
        }

        // ============================================================
        // FLAGS DO DESCRIPTOR
        // ============================================================
        if (descriptor.centered()) {
            stage.centerOnScreen();
        }

        stage.setAlwaysOnTop(descriptor.alwaysOnTop());

        return stage;
    }

    // ============================================================
    // DURAÇÃO
    // ============================================================

    private long getDuration(AlertType type) {
        return switch (type) {
            case SUCCESS -> 3000;
            case INFO, WARNING -> 5000;
            default -> 0;
        };
    }

    // ============================================================
    // FECHAMENTO AUTOMÁTICO - PauseTransition
    // ============================================================

    /**
     * Agendamento de fechamento automático usando PauseTransition.
     *
     * <p><b>Vantagens sobre Thread.sleep:</b></p>
     * <ul>
     *   <li>Roda na JavaFX Timeline (mesma thread de renderização)</li>
     *   <li>Não consome threads do SO</li>
     *   <li>Imune a problemas de concorrência</li>
     *   <li>Pausa automaticamente se a janela for minimizada</li>
     *   <li>Não precisa de Platform.runLater para voltar à UI Thread</li>
     * </ul>
     */
    private void scheduleAutoClose(Stage stage, long delayMs) {
        var pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(event -> {
            if (stage.isShowing()) {
                stage.close();
                LOGGER.log(System.Logger.Level.DEBUG,
                        "Alerta fechado automaticamente após {0}ms", delayMs);
            }
        });
        pause.play();
    }
}