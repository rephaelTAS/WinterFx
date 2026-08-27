// SoundManager.java v4.0 - 2026-08-23
// Totalmente desacoplado - Não busca nada, apenas reproduz o que recebe
package com.ossobo.winterfx.sound;

import com.ossobo.winterfx.anotations.Component;
import com.ossobo.winterfx.sound.enums.SoundType;

import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SoundManager v4.0 — Fachada do módulo sound.
 *
 * <p><b>Princípio:</b> O SoundManager NÃO busca recursos. Ele apenas reproduz.
 * Quem tem o recurso (URL) deve passá-lo diretamente.</p>
 *
 * <p><b>Responsabilidades:</b></p>
 * <ul>
 *   <li>Reproduzir URLs de áudio (MP3, WAV, etc.)</li>
 *   <li>Gerenciar volume global</li>
 *   <li>Gerenciar players ativos</li>
 *   <li>Cache de sons internos do framework (apenas para sons padrão)</li>
 * </ul>
 *
 * <p><b>NÃO FAZ:</b></p>
 * <ul>
 *   <li>❌ Buscar recursos no ResourceRegistry</li>
 *   <li>❌ Resolver IDs de som</li>
 *   <li>❌ Conhecer ViewDescriptor ou qualquer outro descritor</li>
 * </ul>
 *
 * @version 4.0 (23/08/2026) - Totalmente desacoplado
 */
public final class SoundManager {

    private static final System.Logger LOGGER = System.getLogger(SoundManager.class.getName());

    private final Map<String, URL> soundCache = new ConcurrentHashMap<>();
    private final Map<String, MediaPlayer> activePlayers = new ConcurrentHashMap<>();

    private double volumeGlobal = 0.7;
    private boolean enabled = true;

    /**
     * Construtor padrão - Sem dependências externas.
     * Apenas pré-carrega os sons padrão do framework.
     */
    public SoundManager() {
        // Pré-carrega os sons padrão do framework
        for (SoundType type : SoundType.values()) {
            preloadSound(type);
        }

        LOGGER.log(System.Logger.Level.INFO, "SoundManager inicializado com {0} sons padrão",
                SoundType.values().length);
    }

    // ============================================================
    // PRÉ-CARREGAMENTO DE SONS PADRÃO (INTERNOS DO FRAMEWORK)
    // ============================================================

    /**
     * Pré-carrega um som padrão do framework.
     * Os sons devem estar em: /com/ossobo/winterfx/sounds/{id}.mp3
     */
    private void preloadSound(SoundType type) {
        String path = "/com/ossobo/winterfx/sounds/" + type.getSoundId() + ".mp3";
        URL url = getClass().getResource(path);

        if (url != null) {
            soundCache.put(type.getSoundId(), url);
            LOGGER.log(System.Logger.Level.DEBUG, "Som pré-carregado: {0}", type.getSoundId());
        } else {
            LOGGER.log(System.Logger.Level.DEBUG, "Som padrão não encontrado: {0} (pode ser opcional)", type.getSoundId());
        }
    }

    // ============================================================
    // API PÚBLICA — REPRODUÇÃO
    // ============================================================

    /**
     * Reproduz um som padrão do framework por tipo.
     *
     * @param type Tipo de som (INFO, SUCCESS, WARNING, etc.)
     */
    public void play(SoundType type) {
        if (!enabled || type == null) {
            if (type == null) {
                LOGGER.log(System.Logger.Level.WARNING, "SoundType é null");
            }
            return;
        }
        play(type.getSoundId());
    }

    /**
     * Reproduz um som padrão do framework por ID.
     * Busca no cache de sons internos do framework.
     *
     * @param soundId ID do som (ex: "notification-success", "notification-error")
     */
    public void play(String soundId) {
        if (!enabled || soundId == null || soundId.isEmpty()) {
            if (soundId == null) {
                LOGGER.log(System.Logger.Level.WARNING, "soundId é null");
            }
            return;
        }

        // Busca no cache de sons internos do framework
        URL url = soundCache.get(soundId);

        if (url == null) {
            LOGGER.log(System.Logger.Level.DEBUG, "Som não encontrado no cache: {0}", soundId);
            return;
        }

        play(url);
    }

    /**
     * Reproduz um som a partir de uma URL (recurso externo ou passado por quem tem o descritor).
     * Este é o método principal para sons customizados.
     *
     * @param soundUrl URL do arquivo de áudio (pode vir de ViewDescriptor.soundUrl())
     */
    public void play(URL soundUrl) {
        if (!enabled || soundUrl == null) {
            if (soundUrl == null) {
                LOGGER.log(System.Logger.Level.WARNING, "soundUrl é null");
            }
            return;
        }

        Platform.runLater(() -> {
            try {
                playSound(soundUrl);
            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.WARNING, "Falha ao reproduzir som: {0}", soundUrl, e);
            }
        });
    }

    // ============================================================
    // REPRODUÇÃO INTERNA
    // ============================================================

    private void playSound(URL url) {
        if (url == null) {
            LOGGER.log(System.Logger.Level.WARNING, "URL é null");
            return;
        }

        var urlString = url.toExternalForm();

        try {
            if (urlString.toLowerCase().endsWith(".mp3")) {
                var media = new Media(urlString);
                var player = new MediaPlayer(media);
                player.setVolume(volumeGlobal);
                player.setCycleCount(1);

                player.setOnEndOfMedia(() -> {
                    player.stop();
                    player.dispose();
                    activePlayers.remove(urlString);
                });

                player.setOnError(() -> {
                    LOGGER.log(System.Logger.Level.WARNING, "Erro no MediaPlayer para: {0}", urlString);
                    player.dispose();
                    activePlayers.remove(urlString);
                });

                activePlayers.put(urlString, player);
                player.play();
                LOGGER.log(System.Logger.Level.DEBUG, "Reproduzindo MP3: {0}", urlString);

            } else {
                var clip = new AudioClip(urlString);
                clip.setVolume(volumeGlobal);
                clip.setCycleCount(1);
                clip.play();
                LOGGER.log(System.Logger.Level.DEBUG, "Reproduzindo AudioClip: {0}", urlString);
            }

        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "Falha ao instanciar mídia: {0}", urlString, e);
        }
    }

    // ============================================================
    // CONTROLE
    // ============================================================

    /**
     * Define o volume global (0.0 a 1.0).
     */
    public void setVolume(double volume) {
        this.volumeGlobal = Math.max(0.0, Math.min(1.0, volume));
        LOGGER.log(System.Logger.Level.DEBUG, "Volume alterado para: {0}", volumeGlobal);
    }

    public double getVolume() {
        return volumeGlobal;
    }

    /**
     * Para todos os sons em reprodução.
     */
    public void stopAll() {
        activePlayers.values().forEach(player -> {
            try {
                player.stop();
                player.dispose();
            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.WARNING, "Erro ao parar player", e);
            }
        });
        activePlayers.clear();
        LOGGER.log(System.Logger.Level.DEBUG, "Todos os sons parados");
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            stopAll();
        }
        LOGGER.log(System.Logger.Level.DEBUG, "SoundManager {0}", enabled ? "ativado" : "desativado");
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ============================================================
    // CACHE
    // ============================================================

    public void clearCache() {
        soundCache.clear();
        LOGGER.log(System.Logger.Level.DEBUG, "Cache de sons limpo");
    }

    /**
     * Recarrega os sons padrão do framework.
     */
    public void reload() {
        stopAll();
        clearCache();
        for (SoundType type : SoundType.values()) {
            preloadSound(type);
        }
        LOGGER.log(System.Logger.Level.INFO, "Sons recarregados");
    }

    public boolean isSoundAvailable(String soundId) {
        return soundCache.containsKey(soundId);
    }

    public boolean isSoundAvailable(SoundType type) {
        return type != null && isSoundAvailable(type.getSoundId());
    }

    public int getCacheSize() {
        return soundCache.size();
    }
}