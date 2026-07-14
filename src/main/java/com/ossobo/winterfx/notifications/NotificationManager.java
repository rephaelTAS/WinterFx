// NotificationManager.java v8.0 - 2026-07-01
// USA AlertManager em vez de StageManager diretamente
package com.ossobo.winterfx.notifications;

import com.ossobo.winterfx.notifications.enums.NotificationType;
import com.ossobo.winterfx.notifications.resolver.NotificationViewResolver;
import com.ossobo.winterfx.resources.enums.AlertType;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.sound.SoundManager;
import com.ossobo.winterfx.sound.enums.SoundType;
import com.ossobo.winterfx.view.alert.AlertManager;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 🔔 NotificationManager v8.0 — Fachada do módulo notification.
 *
 * <p>USA {@link AlertManager} para exibir alertas.
 * NÃO conhece {@code StageManager} diretamente.</p>
 *
 * <p>Dependência correta: {@code NotificationManager → AlertManager → StageManager}</p>
 *
 * @version 8.0 (01/07/2026)
 */
public class NotificationManager {

    private final Map<String, javafx.stage.Stage> alertasAtivos = new ConcurrentHashMap<>();

    private AlertManager alertManager;
    private final ResourceRegistry resourceRegistry;
    private SoundManager soundManager;

    public NotificationManager(ResourceRegistry resourceRegistry) {
        this.resourceRegistry = resourceRegistry;
    }

    public void setAlertManager(AlertManager alertManager) { this.alertManager = alertManager; }
    public void setSoundManager(SoundManager soundManager) { this.soundManager = soundManager; }

    // =============================================
    // API PÚBLICA
    // =============================================

    public String info(String titulo, String descricao) {
        return criarAlerta(titulo, descricao, null, AlertType.INFO);
    }

    public String success(String titulo, String descricao) {
        return criarAlerta(titulo, descricao, null, AlertType.SUCCESS);
    }

    public String warn(String titulo, String descricao) {
        return criarAlerta(titulo, descricao, null, AlertType.WARNING);
    }

    public String erro(String titulo, String descricao) {
        return criarAlerta(titulo, descricao, null, AlertType.ERROR);
    }

    public String erro(String titulo, String descricao, String detalhes) {
        return criarAlerta(titulo, descricao, detalhes, AlertType.ERROR);
    }

    public String critico(String titulo, String descricao) {
        return criarAlerta(titulo, descricao, null, AlertType.CRITICAL);
    }

    // =============================================
    // GERENCIAMENTO
    // =============================================

    public void fecharAlerta(String id) {
        Platform.runLater(() -> {
            javafx.stage.Stage stage = alertasAtivos.remove(id);
            if (stage != null) stage.close();
        });
    }

    public void fecharTodosAlertas() {
        Platform.runLater(() -> alertasAtivos.values().forEach(s -> s.close()));
        alertasAtivos.clear();
    }

    // =============================================
    // CONFIRMAÇÃO
    // =============================================

    public enum TipoConfirmacao { PADRAO, PERIGOSA, SAIR }

    public void confirmar(String mensagem, String titulo, Consumer<Boolean> callback) {
        confirmarComDetalhes(mensagem, null, titulo, TipoConfirmacao.PADRAO, callback);
    }

    public void confirmarComDetalhes(String mensagem, String detalhes, String titulo,
                                     TipoConfirmacao tipo, Consumer<Boolean> callback) {
        Platform.runLater(() -> {
            try {
                NotificationType notificationType = tipo == TipoConfirmacao.PERIGOSA
                        ? NotificationType.CRITICAL : NotificationType.CONFIRMATION;
                String viewId = NotificationViewResolver.resolveViewId(notificationType);

                if (!resourceRegistry.contains(viewId)) {
                    callback.accept(showNativeConfirm(mensagem, titulo, tipo));
                    return;
                }

                tocarSom(notificationType);
                boolean result = alertManager.showAndWait(viewId);
                callback.accept(result);

            } catch (Exception e) {
                callback.accept(showNativeConfirm(mensagem, titulo, tipo));
            }
        });
    }

    // =============================================
    // INTERNO
    // =============================================

    private String criarAlerta(String titulo, String descricao, String detalhes, AlertType tipo) {
        String id = UUID.randomUUID().toString();

        Platform.runLater(() -> {
            try {
                NotificationType notificationType = convertToNotificationType(tipo);
                String viewId = NotificationViewResolver.resolveViewId(notificationType);

                if (!resourceRegistry.contains(viewId)) {
                    showNativeAlert(titulo, descricao, tipo);
                    return;
                }

                tocarSom(notificationType);

                javafx.stage.Stage stage = alertManager.showAlert(viewId, tipo);
                alertasAtivos.put(id, stage);

            } catch (Exception e) {
                showNativeAlert(titulo, descricao, tipo);
            }
        });

        return id;
    }

    private void tocarSom(NotificationType type) {
        if (soundManager == null) return;
        SoundType soundType = mapToSoundType(type);
        if (soundType != null) soundManager.play(soundType);
    }

    private SoundType mapToSoundType(NotificationType type) {
        return switch (type) {
            case INFO -> SoundType.INFO;
            case SUCCESS -> SoundType.SUCCESS;
            case WARNING -> SoundType.WARNING;
            case ERROR -> SoundType.ERROR;
            case CRITICAL -> SoundType.CRITICAL;
            case CONFIRMATION -> SoundType.CONFIRMATION;
            default -> null;
        };
    }

    private NotificationType convertToNotificationType(AlertType alertType) {
        return switch (alertType) {
            case INFO -> NotificationType.INFO;
            case SUCCESS -> NotificationType.SUCCESS;
            case WARNING -> NotificationType.WARNING;
            case ERROR -> NotificationType.ERROR;
            case CRITICAL -> NotificationType.CRITICAL;
            case CONFIRMATION -> NotificationType.CONFIRMATION;
            default -> NotificationType.INFO;
        };
    }

    // =============================================
    // FALLBACKS NATIVOS
    // =============================================

    private void showNativeAlert(String titulo, String descricao, AlertType tipo) {
        Platform.runLater(() -> {
            Alert.AlertType alertType = switch (tipo) {
                case INFO, SUCCESS -> Alert.AlertType.INFORMATION;
                case WARNING -> Alert.AlertType.WARNING;
                case ERROR, CRITICAL -> Alert.AlertType.ERROR;
                default -> Alert.AlertType.INFORMATION;
            };
            Alert alert = new Alert(alertType);
            alert.setTitle(titulo);
            alert.setHeaderText(titulo);
            alert.setContentText(descricao);
            alert.showAndWait();
        });
    }

    private boolean showNativeConfirm(String mensagem, String titulo, TipoConfirmacao tipo) {
        Alert alert = new Alert(tipo == TipoConfirmacao.PERIGOSA
                ? Alert.AlertType.WARNING : Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(titulo);
        alert.setContentText(mensagem);
        Optional<ButtonType> result = alert.showAndWait();
        return result.filter(r -> r == ButtonType.OK).isPresent();
    }
}