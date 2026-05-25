package com.ossobo.winterfx.notifications.model;

/**
 * 📋 UserFriendlyMessage
 *
 * Representa uma mensagem amigável para o usuário.
 */
public class UserFriendlyMessage {
    private final String title;
    private final String body;
    private final String action;
    private final String context;
    private final int complexity;

    private UserFriendlyMessage(Builder builder) {
        this.title = builder.title;
        this.body = builder.body;
        this.action = builder.action;
        this.context = builder.context;
        this.complexity = builder.complexity;
    }

    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getAction() { return action; }
    public String getContext() { return context; }
    public int getComplexity() { return complexity; }

    public static class Builder {
        private String title;
        private String body;
        private String action;
        private String context;
        private int complexity;

        public Builder title(String title) { this.title = title; return this; }
        public Builder body(String body) { this.body = body; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder context(String context) { this.context = context; return this; }
        public Builder complexity(int complexity) { this.complexity = complexity; return this; }

        public UserFriendlyMessage build() {
            return new UserFriendlyMessage(this);
        }
    }
}