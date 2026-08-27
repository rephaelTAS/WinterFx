// NotificationManager.java v12.0 - 2026-08-23
// Atualizado para usar ResourceModule (Fachada) e remover NotificationViewResolver
package com.ossobo.winterfx.notifications;

import com.ossobo.winterfx.anotations.Component;
import com.ossobo.winterfx.anotations.Inject;
import com.ossobo.winterfx.notifications.enums.NotificationType;
import com.ossobo.winterfx.resources.ResourceModule;
import com.ossobo.winterfx.resources.enums.ResourceType;
import com.ossobo.winterfx.sound.SoundManager;
import com.ossobo.winterfx.sound.enums.SoundType;
import com.ossobo.winterfx.view.alert.AlertManager;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * NotificationManager v12.0 — Fachada do módulo notification.
 *
 * <p><b>Dependências:</b></p>
 * <ul>
 *   <li>ResourceModule (obrigatório) - Fachada de recursos</li>
 *   <li>SoundManager (pode ser null - sem som)</li>
 *   <li>AlertManager (obrigatório, via setter)</li>
 * </ul>
 *
 * @version 12.0 (23/08/2026) - ResourceModule + remoção de NotificationViewResolver
 */
public class NotificationManager {

    private static final System.Logger LOGGER = System.getLogger(NotificationManager.class.getName());

    private final Map<String, javafx.stage.Stage> alertasAtivos = new ConcurrentHashMap<>();
    private final ResourceModule resourceModule;
    private final SoundManager soundManager;

    private AlertManager alertManager;

    /**
     * Construtor com dependências obrigatórias.
     *
     * @param resourceModule Fachada de recursos (obrigatório)
     * @param soundManager Gerenciador de som (pode ser null para desabilitar som)
     */
    public NotificationManager(ResourceModule resourceModule, SoundManager soundManager) {
        this.resourceModule = Objects.requireNonNull(resourceModule, "resourceModule não pode ser null");
        this.soundManager = soundManager; // Pode ser null (sem som)

        if (soundManager == null) {
            LOGGER.log(System.Logger.Level.WARNING, "SoundManager não fornecido - notificações sem som");
        } else {
            LOGGER.log(System.Logger.Level.INFO, "NotificationManager inicializado com suporte a som");
        }
    }

    /**
     * Define o AlertManager (necessário para notificações customizadas).
     */
    @Inject
    public void setAlertManager(AlertManager alertManager) {
        this.alertManager = Objects.requireNonNull(alertManager, "alertManager não pode ser null");
        LOGGER.log(System.Logger.Level.INFO, "AlertManager configurado");
    }

    // =============================================
    // API PÚBLICA
    // =============================================

    public String info(String titulo, String descricao) {
        return criarAlerta(titulo, descricao, null, NotificationType.INFO);
    }

    public String success(String titulo, String descricao) {
        return criarAlerta(titulo, descricao, null, NotificationType.SUCCESS);
    }

    public String warn(String titulo, String descricao) {
        return criarAlerta(titulo, descricao, null, NotificationType.WARNING);
    }

    public String erro(String titulo, String descricao) {
        return criarAlerta(titulo, descricao, null, NotificationType.ERROR);
    }

    public String erro(String titulo, String descricao, String detalhes) {
        return criarAlerta(titulo, descricao, detalhes, NotificationType.ERROR);
    }

    public String critico(String titulo, String descricao) {
        return criarAlerta(titulo, descricao, null, NotificationType.CRITICAL);
    }

    // =============================================
    // CONFIRMAÇÃO - VERSÃO SÍNCRONA (para a JavaFX Thread)
    // =============================================

    /**
     * Confirmação síncrona para uso direto na JavaFX Thread.
     * Usa Alert nativo para garantir bloqueio correto.
     */
    public boolean confirmarSync(String mensagem, String titulo, TipoConfirmacao tipo) {
        var alertType = tipo == TipoConfirmacao.PERIGOSA
                ? Alert.AlertType.WARNING : Alert.AlertType.CONFIRMATION;

        var alert = new Alert(alertType);
        alert.setTitle(titulo);
        alert.setHeaderText(titulo);
        alert.setContentText(mensagem);

        var result = alert.showAndWait();
        return result.filter(r -> r == ButtonType.OK).isPresent();
    }

    // =============================================
    // CONFIRMAÇÃO - VERSÃO ASSÍNCRONA (para Background)
    // =============================================

    public enum TipoConfirmacao { PADRAO, PERIGOSA, SAIR }

    public void confirmar(String mensagem, String titulo, Consumer<Boolean> callback) {
        confirmarComDetalhes(mensagem, null, titulo, TipoConfirmacao.PADRAO, callback);
    }

    public void confirmarComDetalhes(String mensagem, String detalhes, String titulo,
                                     TipoConfirmacao tipo, Consumer<Boolean> callback) {
        Objects.requireNonNull(callback, "callback não pode ser null");

        Platform.runLater(() -> {
            try {
                if (alertManager == null) {
                    LOGGER.log(System.Logger.Level.DEBUG, "AlertManager não disponível - usando fallback nativo");
                    callback.accept(confirmarSync(mensagem, titulo, tipo));
                    return;
                }

                var notificationType = tipo == TipoConfirmacao.PERIGOSA
                        ? NotificationType.CRITICAL : NotificationType.CONFIRMATION;

                // ✅ RESOLVE VIEW ID DIRETAMENTE (sem NotificationViewResolver)
                var viewId = resolveViewId(notificationType);

                // ✅ Usa ResourceModule.exists() com tipo ALERT
                if (!resourceModule.exists(viewId, ResourceType.ALERT)) {
                    LOGGER.log(System.Logger.Level.DEBUG, "View de alerta não encontrada: {0}", viewId);
                    callback.accept(confirmarSync(mensagem, titulo, tipo));
                    return;
                }

                tocarSom(notificationType);

                // showAndWait sempre retorna true - usamos fallback para confirmação real
                // A confirmação real é feita pelo OnConfirmationHandler com Alert nativo
                boolean result = alertManager.showAndWait(viewId);
                callback.accept(result);

            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.WARNING, "Erro na confirmação, usando fallback nativo", e);
                callback.accept(confirmarSync(mensagem, titulo, tipo));
            }
        });
    }

    // =============================================
    // GERENCIAMENTO
    // =============================================

    public void fecharAlerta(String id) {
        Platform.runLater(() -> {
            var stage = alertasAtivos.remove(id);
            if (stage != null) stage.close();
        });
    }

    public void fecharTodosAlertas() {
        Platform.runLater(() -> alertasAtivos.values().forEach(javafx.stage.Stage::close));
        alertasAtivos.clear();
    }

    // =============================================
    // INTERNO
    // =============================================

    /**
     * Resolve o ID da view para um tipo de notificação.
     *
     * <p>Esta é a lógica que antes estava no NotificationViewResolver (deletado).</p>
     */
    private String resolveViewId(NotificationType type) {
        return switch (type) {
            case INFO -> "notification-info";
            case SUCCESS -> "notification-success";
            case WARNING -> "notification-warning";
            case ERROR, EXCEPTION -> "notification-error";
            case CRITICAL -> "notification-critical";
            case CONFIRMATION -> "notification-confirmation";
            default -> "notification-info";
        };
    }

    private String criarAlerta(String titulo, String descricao, String detalhes, NotificationType tipo) {
        var id = UUID.randomUUID().toString();

        Platform.runLater(() -> {
            try {
                if (alertManager == null) {
                    LOGGER.log(System.Logger.Level.WARNING, "AlertManager não disponível - usando fallback nativo");
                    showNativeAlert(titulo, descricao, tipo);
                    return;
                }

                // ✅ RESOLVE VIEW ID DIRETAMENTE (sem NotificationViewResolver)
                var viewId = resolveViewId(tipo);

                // ✅ Usa ResourceModule.exists() com tipo ALERT
                if (!resourceModule.exists(viewId, ResourceType.ALERT)) {
                    LOGGER.log(System.Logger.Level.DEBUG, "View de alerta não encontrada: {0}", viewId);
                    showNativeAlert(titulo, descricao, tipo);
                    return;
                }

                tocarSom(tipo);

                var stage = alertManager.showAlert(viewId, convertToAlertType(tipo));
                alertasAtivos.put(id, stage);

            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.WARNING, "Erro ao criar alerta, usando fallback nativo", e);
                showNativeAlert(titulo, descricao, tipo);
            }
        });

        return id;
    }

    private void tocarSom(NotificationType type) {
        if (soundManager == null) {
            LOGGER.log(System.Logger.Level.DEBUG, "SoundManager não disponível - sem som");
            return;
        }

        var soundType = mapToSoundType(type);
        if (soundType != null) {
            soundManager.play(soundType);
        }
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

    private com.ossobo.winterfx.resources.enums.AlertType convertToAlertType(NotificationType notificationType) {
        return switch (notificationType) {
            case INFO -> com.ossobo.winterfx.resources.enums.AlertType.INFO;
            case SUCCESS -> com.ossobo.winterfx.resources.enums.AlertType.SUCCESS;
            case WARNING -> com.ossobo.winterfx.resources.enums.AlertType.WARNING;
            case ERROR, EXCEPTION -> com.ossobo.winterfx.resources.enums.AlertType.ERROR;
            case CRITICAL -> com.ossobo.winterfx.resources.enums.AlertType.CRITICAL;
            case CONFIRMATION -> com.ossobo.winterfx.resources.enums.AlertType.CONFIRMATION;
            default -> com.ossobo.winterfx.resources.enums.AlertType.INFO;
        };
    }

    // =============================================
    // FALLBACKS NATIVOS
    // =============================================

    private void showNativeAlert(String titulo, String descricao, NotificationType tipo) {
        Platform.runLater(() -> {
            var alertType = switch (tipo) {
                case INFO, SUCCESS -> Alert.AlertType.INFORMATION;
                case WARNING -> Alert.AlertType.WARNING;
                case ERROR, EXCEPTION, CRITICAL -> Alert.AlertType.ERROR;
                default -> Alert.AlertType.INFORMATION;
            };
            var alert = new Alert(alertType);
            alert.setTitle(titulo);
            alert.setHeaderText(titulo);
            alert.setContentText(descricao);
            alert.showAndWait();
        });
    }
}