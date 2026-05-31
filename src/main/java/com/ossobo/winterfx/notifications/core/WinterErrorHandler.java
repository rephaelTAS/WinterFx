package com.ossobo.winterfx.notifications.core;

import com.ossobo.winterfx.notifications.NotificationManager;
import com.ossobo.winterfx.notifications.model.UserFriendlyMessage;
import com.ossobo.winterfx.anotations.Component;
import com.ossobo.winterfx.anotations.Inject;
import com.ossobo.winterfx.notifications.enums.NotificationType;

/**
 * 🛡️ WinterFX Error Handler v12
 *
 * Propósito: Coordenar o tratamento de erros usando o NotificationManager v4.0.
 *
 * Princípio: "Orquestração sobre implementação"
 *
 * <p><b>🔥 v12:</b> Atualizado para NotificationManager v4.0 (info, warn, erro, critical).</p>
 */
@Component
public class WinterErrorHandler {

    private static final int COMPREHENSION_TIME_LIMIT_MS = 3000;

    @Inject
    private NotificationManager nm;

    private final ErrorTranslator translator = new ErrorTranslator();
    private final ComplexityCalculator calculator = new ComplexityCalculator();
    private final AlertDisplay display = new AlertDisplay();

    public void registrarErro(Exception ex, String contexto) {
        try {
            NotificationType level = determinarNivelAlerta(ex);
            UserFriendlyMessage message = translator.criarMensagem(ex, contexto);
            message = calculator.validarCompreensao(message);
            display.exibir(level, message, nm);
        } catch (Exception handlerError) {
            display.fallback(ex, contexto, handlerError, nm);
        }
    }

    private NotificationType determinarNivelAlerta(Exception ex) {
        String className = ex.getClass().getSimpleName().toLowerCase();
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (className.contains("sql") || message.contains("connection")) return NotificationType.ERROR;
        if (className.contains("validation") || message.contains("invalid")) return NotificationType.ERROR;
        if (className.contains("business") || message.contains("rule")) return NotificationType.WARNING;
        return NotificationType.INFO;
    }

    private static class ErrorTranslator {
        UserFriendlyMessage criarMensagem(Exception ex, String contexto) {
            return new UserFriendlyMessage.Builder()
                    .title(formatarTitulo(contexto))
                    .body(traduzirParaUsuario(ex.getMessage(), ex))
                    .action(sugerirAcao(ex))
                    .context(contexto)
                    .build();
        }

        private String traduzirParaUsuario(String technicalMessage, Exception ex) {
            if (technicalMessage == null) return "Ocorreu um erro inesperado.";
            String lowerMessage = technicalMessage.toLowerCase();
            if (lowerMessage.contains("connection refused")) return "Servidor indisponível. Tente novamente.";
            if (lowerMessage.contains("null pointer")) return "Informação não encontrada.";
            if (lowerMessage.contains("sql")) return "Problema ao processar dados.";
            if (lowerMessage.contains("invalid")) return "Dados inválidos.";
            return technicalMessage.length() > 100 ? technicalMessage.substring(0, 100) + "..." : technicalMessage;
        }

        private String sugerirAcao(Exception ex) {
            String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            if (message.contains("connection")) return "Verifique sua conexão.";
            if (message.contains("invalid")) return "Corrija os dados.";
            return "Tente novamente. Se persistir, contate o suporte.";
        }

        private String formatarTitulo(String contexto) {
            if (contexto == null || contexto.isEmpty()) return "Erro no Sistema";
            if (contexto.contains(".")) contexto = contexto.split("\\.")[contexto.split("\\.").length - 1];
            return contexto.replaceAll("([a-z])([A-Z])", "$1 $2") + " - Erro";
        }
    }

    private static class ComplexityCalculator {
        private static final int MS_POR_PALAVRA = 275;

        UserFriendlyMessage validarCompreensao(UserFriendlyMessage message) {
            int complexidade = calcularComplexidade(message.getBody());
            if (complexidade > COMPREHENSION_TIME_LIMIT_MS) {
                String corpoSimplificado = simplificarMensagem(message.getBody());
                return new UserFriendlyMessage.Builder()
                        .title(message.getTitle()).body(corpoSimplificado)
                        .action(message.getAction()).context(message.getContext())
                        .complexity(calcularComplexidade(corpoSimplificado)).build();
            }
            return message;
        }

        private int calcularComplexidade(String mensagem) {
            if (mensagem == null || mensagem.trim().isEmpty()) return 0;
            String[] palavras = mensagem.trim().split("\\s+");
            int estimatedTime = palavras.length * MS_POR_PALAVRA;
            estimatedTime += contarTermosTecnicos(mensagem) * 100;
            if (palavras.length > 15) estimatedTime += 150;
            return Math.max(500, Math.min(estimatedTime, 5000));
        }

        private int contarTermosTecnicos(String texto) {
            if (texto == null) return 0;
            String lowerText = texto.toLowerCase();
            String[] techTerms = {"sql", "database", "api", "exception", "error"};
            int count = 0;
            for (String term : techTerms) { if (lowerText.contains(term)) count++; }
            return count;
        }

        private String simplificarMensagem(String mensagem) {
            if (mensagem == null || mensagem.length() <= 100) return mensagem;
            String simplified = mensagem.substring(0, 100).trim();
            if (!simplified.endsWith(".") && !simplified.endsWith("!")) simplified += "...";
            return simplified;
        }
    }

    private class AlertDisplay {
        void exibir(NotificationType level, UserFriendlyMessage message, NotificationManager manager) {
            try {
                String titulo = message.getTitle();
                String corpo = message.getBody();

                switch (level) {
                    case ERROR   -> manager.erro(titulo, corpo);
                    case WARNING -> manager.warn(titulo, corpo);
                    case SUCCESS -> manager.info(titulo, corpo);
                    default      -> manager.info(titulo, corpo);
                }
            } catch (Exception e) {
                fallbackWinterFx(message);
            }
        }

        void fallback(Exception originalError, String contexto, Throwable handlerError,
                      NotificationManager manager) {
            try {
                manager.erro("Erro no Sistema", "Ocorreu um erro crítico.");
            } catch (Exception e) {
                System.err.println("❌ FALLBACK CRÍTICO: " + originalError.getMessage());
                if (handlerError != null) System.err.println("   Handler error: " + handlerError.getMessage());
            }
        }

        private void fallbackWinterFx(UserFriendlyMessage message) {
            System.err.println("⚠️ FALLBACK - Título: " + message.getTitle());
            System.err.println("   Mensagem: " + message.getBody());
        }
    }
}