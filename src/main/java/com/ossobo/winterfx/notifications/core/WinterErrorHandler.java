// WinterErrorHandler.java v3.0 - 2026-08-22
// Sem catches vazios e sem fallbackWinterFx()
package com.ossobo.winterfx.notifications.core;

import com.ossobo.winterfx.anotations.Component;
import com.ossobo.winterfx.anotations.Inject;
import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.enums.NotificationType;
import com.ossobo.winterfx.notifications.model.UserFriendlyMessage;

import java.util.Objects;

/**
 * WinterFX Error Handler - Sem catches vazios.
 *
 * @version 3.0 (22/08/2026) - Removido fallbackWinterFx() e catches vazios
 */
@Component
public class WinterErrorHandler {

    private static final System.Logger LOGGER = System.getLogger(WinterErrorHandler.class.getName());

    @Inject
    private NotificationManager notificationManager;

    public void registrarErro(Exception ex, String contexto) {
        Objects.requireNonNull(ex, "ex não pode ser null");

        try {
            var level = determinarNivelAlerta(ex);
            var message = criarMensagem(ex, contexto);
            exibirNotificacao(level, message);
        } catch (Exception handlerError) {
            // Fail-Fast: nunca engolir exceções
            LOGGER.log(System.Logger.Level.ERROR, "Falha ao processar erro: " + contexto, handlerError);
            exibirFallback(ex);
        }
    }

    private NotificationType determinarNivelAlerta(Exception ex) {
        var className = ex.getClass().getSimpleName().toLowerCase();
        var message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        if (className.contains("sql") || message.contains("connection")) {
            return NotificationType.ERROR;
        }
        if (className.contains("validation") || message.contains("invalid")) {
            return NotificationType.ERROR;
        }
        if (className.contains("business") || message.contains("rule")) {
            return NotificationType.WARNING;
        }
        return NotificationType.INFO;
    }

    private UserFriendlyMessage criarMensagem(Exception ex, String contexto) {
        var titulo = formatarTitulo(contexto);
        var corpo = traduzirParaUsuario(ex.getMessage(), ex);
        var acao = sugerirAcao(ex);

        return new UserFriendlyMessage.Builder()
                .title(titulo)
                .body(corpo)
                .action(acao)
                .context(contexto)
                .build();
    }

    private String traduzirParaUsuario(String technicalMessage, Exception ex) {
        if (technicalMessage == null) {
            return "Ocorreu um erro inesperado.";
        }

        var lower = technicalMessage.toLowerCase();
        if (lower.contains("connection refused")) return "Servidor indisponível. Tente novamente.";
        if (lower.contains("null pointer")) return "Informação não encontrada.";
        if (lower.contains("sql")) return "Problema ao processar dados.";
        if (lower.contains("invalid")) return "Dados inválidos.";

        return technicalMessage.length() > 100 ? technicalMessage.substring(0, 100) + "..." : technicalMessage;
    }

    private String sugerirAcao(Exception ex) {
        var message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (message.contains("connection")) return "Verifique sua conexão.";
        if (message.contains("invalid")) return "Corrija os dados.";
        return "Tente novamente. Se persistir, contate o suporte.";
    }

    private String formatarTitulo(String contexto) {
        if (contexto == null || contexto.isEmpty()) return "Erro no Sistema";
        if (contexto.contains(".")) {
            contexto = contexto.split("\\.")[contexto.split("\\.").length - 1];
        }
        return contexto.replaceAll("([a-z])([A-Z])", "$1 $2") + " - Erro";
    }

    private void exibirNotificacao(NotificationType level, UserFriendlyMessage message) {
        if (notificationManager == null) {
            LOGGER.log(System.Logger.Level.WARNING, "NotificationManager não disponível");
            return;
        }

        switch (level) {
            case ERROR -> notificationManager.erro(message.getTitle(), message.getBody());
            case WARNING -> notificationManager.warn(message.getTitle(), message.getBody());
            case SUCCESS -> notificationManager.info(message.getTitle(), message.getBody());
            default -> notificationManager.info(message.getTitle(), message.getBody());
        }
    }

    /**
     * Fallback de emergência - NUNCA engole exceções.
     */
    private void exibirFallback(Exception ex) {
        // Log é OBRIGATÓRIO
        LOGGER.log(System.Logger.Level.ERROR, "Erro crítico no sistema", ex);

        // Tenta notificar o usuário
        if (notificationManager != null) {
            try {
                notificationManager.erro("Erro Crítico", "Ocorreu um erro no sistema. Verifique os logs.");
            } catch (Exception e) {
                // Último recurso: log apenas
                LOGGER.log(System.Logger.Level.ERROR, "Falha ao exibir fallback", e);
            }
        }
    }
}