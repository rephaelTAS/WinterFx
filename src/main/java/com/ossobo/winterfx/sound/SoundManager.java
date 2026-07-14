// SoundManager.java v2.0
package com.ossobo.winterfx.sound;

import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.sound.enums.SoundType;

import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎵 SoundManager v2.0 — Fachada do módulo sound.
 *
 * <p>Gerencia sons e efeitos sonoros. Os sons são obtidos
 * dos {@link ViewDescriptor} registrados no {@link ResourceRegistry}.</p>
 *
 * <p>NÃO conhece módulos externos além de {@code resources} e {@code scanner}.</p>
 *
 * @version 2.0 (01/07/2026)
 */
public final class SoundManager {

    private static final SoundManager INSTANCE = new SoundManager();

    public static SoundManager getInstance() {
        return INSTANCE;
    }

    private SoundManager() {}

    // ============================================================
    // DEPENDÊNCIAS
    // ============================================================

    private ResourceRegistry resourceRegistry;
    private double volumeGlobal = 0.7;
    private boolean enabled = true;

    // ============================================================
    // CACHE
    // ============================================================

    private final Map<String, URL> soundCache = new ConcurrentHashMap<>();
    private final Map<String, MediaPlayer> activePlayers = new ConcurrentHashMap<>();

    // ============================================================
    // INICIALIZAÇÃO
    // ============================================================

    public void initialize(ResourceRegistry registry) {
        this.resourceRegistry = registry;
        for (SoundType type : SoundType.values()) {
            preloadSound(type);
        }
    }

    private void preloadSound(SoundType type) {
        URL url = resolveSoundUrl(type.getSoundId());
        if (url != null) {
            soundCache.put(type.getSoundId(), url);
        }
    }

    // ============================================================
    // RESOLUÇÃO DE URL
    // ============================================================

    private URL resolveSoundUrl(String soundId) {
        if (resourceRegistry == null) return null;

        return resourceRegistry.findById(soundId)
                .filter(d -> d instanceof ViewDescriptor)
                .map(d -> ((ViewDescriptor) d).getSoundUrl())
                .orElse(null);
    }

    // ============================================================
    // API PÚBLICA — REPRODUÇÃO
    // ============================================================

    public void play(SoundType type) {
        if (!enabled || type == null) return;
        play(type.getSoundId());
    }

    public void play(String soundId) {
        if (!enabled || soundId == null || soundId.isEmpty()) return;

        Platform.runLater(() -> {
            try {
                URL url = soundCache.computeIfAbsent(soundId, this::resolveSoundUrl);
                if (url != null) {
                    playSound(url);
                }
            } catch (Exception ignored) {}
        });
    }

    public void play(URL soundUrl) {
        if (!enabled || soundUrl == null) return;
        Platform.runLater(() -> playSound(soundUrl));
    }

    // ============================================================
    // REPRODUÇÃO INTERNA
    // ============================================================

    private void playSound(URL url) {
        if (url == null) return;

        String urlString = url.toExternalForm();

        try {
            if (urlString.toLowerCase().endsWith(".mp3")) {
                Media media = new Media(urlString);
                MediaPlayer player = new MediaPlayer(media);
                player.setVolume(volumeGlobal);
                player.setCycleCount(1);

                player.setOnEndOfMedia(() -> {
                    player.stop();
                    player.dispose();
                    activePlayers.remove(urlString);
                });

                player.setOnError(() -> {
                    player.dispose();
                    activePlayers.remove(urlString);
                });

                activePlayers.put(urlString, player);
                player.play();
            } else {
                AudioClip clip = new AudioClip(urlString);
                clip.setVolume(volumeGlobal);
                clip.setCycleCount(1);
                clip.play();
            }
        } catch (Exception ignored) {}
    }

    // ============================================================
    // CONTROLE
    // ============================================================

    public void setVolume(double volume) {
        this.volumeGlobal = Math.max(0.0, Math.min(1.0, volume));
    }

    public double getVolume() {
        return volumeGlobal;
    }

    public void stopAll() {
        activePlayers.values().forEach(player -> {
            try { player.stop(); player.dispose(); } catch (Exception ignored) {}
        });
        activePlayers.clear();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) stopAll();
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ============================================================
    // CACHE
    // ============================================================

    public void clearCache() {
        soundCache.clear();
    }

    public void reload() {
        stopAll();
        clearCache();
        for (SoundType type : SoundType.values()) {
            preloadSound(type);
        }
    }

    public boolean isSoundAvailable(String soundId) {
        return soundCache.containsKey(soundId);
    }

    public boolean isSoundAvailable(SoundType type) {
        return type != null && isSoundAvailable(type.getSoundId());
    }
}