package com.ossobo.winterfx.notifications.types;

/**
 * NotificationType v1.0
 * Classifica o tipo da notificação.
 */
public enum NotificationType {
    INFO("info", "ℹ️", "info-sound", 3000),
    WARNING("warning", "⚠️", "warning-sound", 4000),
    ERROR("error", "❌", "error-sound", 0),        // 0 = persistente
    SUCCESS("success", "✅", "success-sound", 2500),
    CONFIRMATION("confirmation", "❓", null, 0),
    DETAIL("detail", "📋", null, 0),
    CUSTOM("custom", null, null, 3000);

    private final String styleClass;
    private final String defaultEmoji;
    private final String defaultSoundId;
    private final long defaultTimeout;

    NotificationType(String styleClass, String defaultEmoji,
                     String defaultSoundId, long defaultTimeout) {
        this.styleClass = styleClass;
        this.defaultEmoji = defaultEmoji;
        this.defaultSoundId = defaultSoundId;
        this.defaultTimeout = defaultTimeout;
    }

    public String getStyleClass() { return styleClass; }
    public String getDefaultEmoji() { return defaultEmoji; }
    public String getDefaultSoundId() { return defaultSoundId; }
    public long getDefaultTimeout() { return defaultTimeout; }
}
