// SoundType.java
package com.ossobo.winterfx.sound.enums;

/**
 * 🎵 Tipos de sons disponíveis no WinterFX.
 *
 * <p>Cada tipo mapeia para um ID de som registrado no ResourceRegistry.</p>
 *
 * @version 2.0 (01/07/2026)
 */
public enum SoundType {

    INFO("sound-info", "Som de informação"),
    SUCCESS("sound-success", "Som de sucesso"),
    WARNING("sound-warning", "Som de aviso"),
    ERROR("sound-error", "Som de erro"),
    CRITICAL("sound-critical", "Som crítico"),
    CONFIRMATION("sound-confirmation", "Som de confirmação");

    private final String soundId;
    private final String description;

    SoundType(String soundId, String description) {
        this.soundId = soundId;
        this.description = description;
    }

    public String getSoundId() { return soundId; }
    public String getDescription() { return description; }

    /**
     * Retorna o SoundType pelo ID do som.
     */
    public static SoundType fromSoundId(String soundId) {
        for (SoundType type : values()) {
            if (type.soundId.equals(soundId)) {
                return type;
            }
        }
        return null;
    }
}