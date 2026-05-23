package com.ossobo.winterfx.core.componnet;

/**
 * MENSAGEM AMIGÁVEL AO USUÁRIO
 * Propósito: Representar mensagens de erro/sucesso otimizadas para UX
 * Princípio: Imutabilidade + métodos de transformação
 *
 * REGRAS:
 * 1. Body ≤ 100 caracteres (pós-simplificação)
 * 2. Complexidade ≤ 3000ms
 * 3. Sempre incluir ação sugerida quando aplicável
 */
public class UserFriendlyMessage {
    private final String title;
    private final String body;
    private final String action;
    private final String context;
    private final int complexity; // ms estimados para compreensão
    private final int wordCount;

    // ===== CONSTRUTOR PRIVADO (USAR BUILDER) =====
    private UserFriendlyMessage(Builder builder) {
        this.title = builder.title;
        this.body = builder.body;
        this.action = builder.action;
        this.context = builder.context;
        this.complexity = builder.complexity;
        this.wordCount = builder.wordCount;
    }

    // ===== GETTERS (SEM SETTERS - IMUTÁVEL) =====
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getAction() { return action; }
    public String getContext() { return context; }
    public int getComplexidade() { return complexity; }
    public int getWordCount() { return wordCount; }

    // ===== BUILDER PATTERN =====
    public static class Builder {
        private String title;
        private String body;
        private String action = "";
        private String context = "Sistema";
        private int complexity = 0;
        private int wordCount = 0;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            this.wordCount = body != null ? body.split("\\s+").length : 0;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder context(String context) {
            this.context = context;
            return this;
        }

        public Builder complexity(int complexity) {
            this.complexity = complexity;
            return this;
        }

        public UserFriendlyMessage build() {
            validate();
            return new UserFriendlyMessage(this);
        }

        private void validate() {
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("Title is required");
            }
            if (body == null || body.trim().isEmpty()) {
                throw new IllegalArgumentException("Body is required");
            }
            if (complexity < 0) {
                throw new IllegalArgumentException("Complexity cannot be negative");
            }
        }
    }

    // ===== MÉTODOS DE TRANSFORMAÇÃO (RETORNAM NOVAS INSTÂNCIAS) =====

    /**
     * Simplifica a mensagem para ≤100 caracteres
     */
    public UserFriendlyMessage simplify() {
        if (body.length() <= 100) {
            return this; // Já está simples o suficiente
        }

        String simplifiedBody = body.substring(0, 100).trim();
        if (!simplifiedBody.endsWith(".") &&
                !simplifiedBody.endsWith("!") &&
                !simplifiedBody.endsWith("?")) {
            simplifiedBody += "...";
        }

        return new Builder()
                .title(title)
                .body(simplifiedBody)
                .action(action)
                .context(context)
                .complexity(complexity / 2) // Simplificação reduz complexidade
                .build();
    }

    /**
     * Adiciona ação à mensagem
     */
    public UserFriendlyMessage withAction(String newAction) {
        return new Builder()
                .title(title)
                .body(body)
                .action(newAction)
                .context(context)
                .complexity(complexity + 500) // Ação adicional aumenta complexidade
                .build();
    }

    /**
     * Define novo contexto
     */
    public UserFriendlyMessage withContext(String newContext) {
        return new Builder()
                .title(title)
                .body(body)
                .action(action)
                .context(newContext)
                .complexity(complexity)
                .build();
    }

    // ===== MÉTODOS DE UTILIDADE =====

    public boolean isTooComplex() {
        return complexity > 3000; // 3 segundos limite
    }

    public boolean hasAction() {
        return action != null && !action.trim().isEmpty();
    }

    public String getFullMessage() {
        if (hasAction()) {
            return body + "\n\n" + action;
        }
        return body;
    }

    public boolean isCritical() {
        return title != null &&
                (title.toLowerCase().contains("crítico") ||
                        title.toLowerCase().contains("critical") ||
                        title.toLowerCase().contains("falha"));
    }

    // ===== FACTORY METHODS =====

    public static UserFriendlyMessage createError(String context, String technicalMessage) {
        return new Builder()
                .title(context + " - Erro")
                .body("Ocorreu um erro: " + truncate(technicalMessage, 80))
                .action("Tente novamente ou contate o suporte.")
                .context(context)
                .complexity(1500)
                .build();
    }

    public static UserFriendlyMessage createSuccess(String context, String message) {
        return new Builder()
                .title(context + " - Sucesso")
                .body(message)
                .action("")
                .context(context)
                .complexity(1000)
                .build();
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    // ===== TOSTRING PARA DEBUG =====
    @Override
    public String toString() {
        return String.format(
                "UserFriendlyMessage[title=%s, words=%d, complexity=%dms, context=%s]",
                title, wordCount, complexity, context
        );
    }
}
