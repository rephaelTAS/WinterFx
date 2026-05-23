// ===== PACOTE: com.nexusfx.estoque.core =====
package com.ossobo.winterfx.core;

import com.ossobo.winterfx.di.annotations.Component;
import com.ossobo.winterfx.WinterFX;
import com.ossobo.winterfx.core.componnet.AlertLevel;
import com.ossobo.winterfx.core.componnet.UserFriendlyMessage;
import javafx.scene.Node;

/**
 * NEXUSFX COMPLIANT ERROR HANDLER - Classe coesa (≤200 linhas)
 * Propósito: Coordenar o tratamento de erro conforme regras do NexusFX
 * Princípio: "Orquestração sobre implementação"
 */
@Component
public class NexusErrorHandler {
    // ===== CONSTANTES =====
    private static final int COMPREHENSION_TIME_LIMIT_MS = 3000;
    private final ErrorTranslator translator = new ErrorTranslator();
    private final ComplexityCalculator calculator = new ComplexityCalculator();
    private final AlertDisplay display = new AlertDisplay();

    // ===== REGISTRAR_ERRO =====
    /**
     * Ponto único de entrada para tratamento de erros.
     * Orquestra o fluxo completo sem implementar detalhes.
     */
    public void registrarErro(Exception ex, String contexto, Node ownerNode) {
        try {
            // 1. Determinar nível
            AlertLevel level = determinarNivelAlerta(ex);

            // 2. Criar mensagem amigável
            UserFriendlyMessage message = translator.criarMensagem(ex, contexto);

            // 3. Validar tempo de compreensão
            message = calculator.validarCompreensao(message);

            // 4. Exibir via NexusFX
            display.exibir(level, message, ownerNode);

        } catch (Exception handlerError) {
            // Fallback mínimo
            display.fallback(ex, contexto, handlerError);
        }
    }

    // ===== DETERMINAR_NIVEL_ALERTA =====
    /**
     * Determina o nível de alerta baseado na exceção.
     * Mantido aqui por ser regra central do handler.
     */
    private AlertLevel determinarNivelAlerta(Exception ex) {
        String className = ex.getClass().getSimpleName().toLowerCase();
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        if (className.contains("sql") || message.contains("connection")) {
            return AlertLevel.CRITICAL;
        }
        if (className.contains("validation") || message.contains("invalid")) {
            return AlertLevel.ERROR;
        }
        if (className.contains("business") || message.contains("rule")) {
            return AlertLevel.WARN;
        }
        return AlertLevel.INFO;
    }

    // ===== CLASSE INTERNA: ERROR_TRANSLATOR =====
    /**
     * Responsabilidade única: Traduzir exceções técnicas para linguagem do usuário.
     */
    private static class ErrorTranslator {
        UserFriendlyMessage criarMensagem(Exception ex, String contexto) {
            String userMessage = traduzirParaUsuario(ex.getMessage(), ex);
            String acaoSugerida = sugerirAcao(ex);

            return new UserFriendlyMessage.Builder()
                    .title(formatarTitulo(contexto))
                    .body(userMessage)
                    .action(acaoSugerida)
                    .context(contexto)
                    .build();
        }

        private String traduzirParaUsuario(String technicalMessage, Exception ex) {
            if (technicalMessage == null) {
                return "Ocorreu um erro inesperado. Nossa equipe foi notificada.";
            }

            String lowerMessage = technicalMessage.toLowerCase();

            if (lowerMessage.contains("connection refused")) {
                return "Servidor indisponível no momento. Tente novamente em instantes.";
            }
            if (lowerMessage.contains("null pointer")) {
                return "Informação necessária não encontrada no sistema.";
            }
            if (lowerMessage.contains("sql")) {
                return "Problema ao processar dados. Contate o administrador.";
            }
            if (lowerMessage.contains("invalid")) {
                return "Dados inválidos fornecidos. Verifique as informações.";
            }

            // Fallback genérico
            return technicalMessage.length() > 100
                    ? technicalMessage.substring(0, 100) + "..."
                    : technicalMessage;
        }

        private String sugerirAcao(Exception ex) {
            String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

            if (message.contains("connection")) {
                return "Verifique sua conexão com a internet.";
            }
            if (message.contains("invalid")) {
                return "Corrija os dados nos campos destacados.";
            }
            return "Tente novamente. Se persistir, contate o suporte.";
        }

        private String formatarTitulo(String contexto) {
            if (contexto == null || contexto.isEmpty()) {
                return "Erro no Sistema";
            }
            if (contexto.contains(".")) {
                String[] parts = contexto.split("\\.");
                contexto = parts[parts.length - 1];
            }
            return contexto.replaceAll("([a-z])([A-Z])", "$1 $2") + " - Erro";
        }
    }

    // ===== CLASSE INTERNA: COMPLEXITY_CALCULATOR =====
    /**
     * Responsabilidade única: Calcular e validar complexidade de mensagens.
     */
    private static class ComplexityCalculator {
        private static final int MS_POR_PALAVRA = 275;

        UserFriendlyMessage validarCompreensao(UserFriendlyMessage message) {
            int complexidade = calcularComplexidade(message.getBody());

            if (complexidade > COMPREHENSION_TIME_LIMIT_MS) {
                String corpoSimplificado = simplificarMensagem(message.getBody());
                return new UserFriendlyMessage.Builder()
                        .title(message.getTitle())
                        .body(corpoSimplificado)
                        .action(message.getAction())
                        .context(message.getContext())
                        .complexity(calcularComplexidade(corpoSimplificado))
                        .build();
            }
            return message;
        }

        private int calcularComplexidade(String mensagem) {
            if (mensagem == null || mensagem.trim().isEmpty()) return 0;

            String[] palavras = mensagem.trim().split("\\s+");
            int estimatedTime = palavras.length * MS_POR_PALAVRA;

            // Penalidades
            estimatedTime += contarTermosTecnicos(mensagem) * 100;
            if (palavras.length > 15) estimatedTime += 150;

            return Math.max(500, Math.min(estimatedTime, 5000));
        }

        private int contarTermosTecnicos(String texto) {
            if (texto == null) return 0;
            String lowerText = texto.toLowerCase();
            String[] techTerms = {"sql", "database", "api", "exception", "error"};

            int count = 0;
            for (String term : techTerms) {
                if (lowerText.contains(term)) count++;
            }
            return count;
        }

        private String simplificarMensagem(String mensagem) {
            if (mensagem == null || mensagem.length() <= 100) return mensagem;

            String simplified = mensagem.substring(0, 100).trim();
            if (!simplified.endsWith(".") && !simplified.endsWith("!")) {
                simplified += "...";
            }
            return simplified;
        }
    }

    // ===== CLASSE INTERNA: ALERT_DISPLAY =====
    /**
     * Responsabilidade única: Exibir alertas via NexusFX.
     */
    private static class AlertDisplay {
        void exibir(AlertLevel level, UserFriendlyMessage message, Node ownerNode) {
            try {
                String corpo = message.getBody();
                if (message.getAction() != null && !message.getAction().isEmpty()) {
                    corpo += "\n\n" + message.getAction();
                }

                switch (level) {
                    case CRITICAL -> WinterFX.alerts().critical(message.getTitle(), corpo, message.getContext());
                    case ERROR -> WinterFX.alerts().erro(message.getTitle(), corpo, message.getContext());
                    case WARN -> WinterFX.alerts().warn(message.getTitle(), corpo, message.getContext());
                    case INFO -> WinterFX.alerts().info(message.getTitle(), corpo, message.getContext());
                }
            } catch (Exception e) {
                fallbackNexusFX(message);
            }
        }

        void fallback(Exception originalError, String contexto, Exception handlerError) {
            try {
                WinterFX.alerts().critical(
                        "Erro no Sistema",
                        "Ocorreu um erro crítico. Contate o suporte.",
                        "NexusErrorHandler"
                );
            } catch (Exception e) {
                // Estado crítico - nada mais a fazer
            }
        }

        private void fallbackNexusFX(UserFriendlyMessage message) {
            System.err.println("FALLBACK - Título: " + message.getTitle());
            System.err.println("FALLBACK - Mensagem: " + message.getBody());
        }
    }
}
