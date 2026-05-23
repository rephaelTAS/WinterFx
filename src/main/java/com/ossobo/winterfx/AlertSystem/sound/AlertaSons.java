package com.ossobo.winterfx.AlertSystem.sound;

import com.ossobo.winterfx.AlertSystem.model.TipoAlerta;
import com.ossobo.winterfx.AlertSystem.model.TipoConfirmacao;
import com.ossobo.winterfx.resources.api.ResourceAPI;
import com.ossobo.winterfx.resources.enums.ResourceType;
import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class AlertaSons {

    private static final Map<String, URL> sonsCache = new HashMap<>();
    private static final Map<String, MediaPlayer> playersAtivos = new HashMap<>();

    private static ResourceAPI resourceAPI;
    private static double volumeGeral = 0.7;
    private static boolean inicializado = false;

    private static final Map<String, String> SOM_PARA_RESOURCE_ID = new HashMap<>();

    static {
        SOM_PARA_RESOURCE_ID.put("info", "fx-sound-info");
        SOM_PARA_RESOURCE_ID.put("warn", "fx-sound-warning");
        SOM_PARA_RESOURCE_ID.put("erro", "fx-sound-error");
        SOM_PARA_RESOURCE_ID.put("critical", "fx-sound-critical");
        SOM_PARA_RESOURCE_ID.put("confirmation", "fx-sound-confirmation");
        SOM_PARA_RESOURCE_ID.put("confirmacao_padrao", "fx-sound-confirmation");
        SOM_PARA_RESOURCE_ID.put("confirmacao_perigo", "fx-sound-warning");
        SOM_PARA_RESOURCE_ID.put("confirmacao_aviso", "fx-sound-warning");
        SOM_PARA_RESOURCE_ID.put("confirmacao_info", "fx-sound-info");
        SOM_PARA_RESOURCE_ID.put("confirmacao_sucesso", "fx-sound-info");
    }

    private AlertaSons() {}

    public static void setResourceAPI(ResourceAPI api) {
        resourceAPI = api;
    }

    private static URL obterUrlDoResource(String nomeSom) {
        if (resourceAPI == null) return null;

        String resourceId = SOM_PARA_RESOURCE_ID.get(nomeSom);
        if (resourceId == null) return null;

        try {
            if (resourceAPI.exists(resourceId, ResourceType.SOUND)) {
                return resourceAPI.getSoundUrl(resourceId);
            }
        } catch (Exception e) {
            System.err.println("Falha ao buscar som '" + resourceId + "': " + e.getMessage());
        }
        return null;
    }

    public static synchronized void inicializar() {
        if (inicializado) return;

        carregarSomPorNome("info");
        carregarSomPorNome("warn");
        carregarSomPorNome("erro");
        carregarSomPorNome("critical");
        carregarSomPorNome("confirmation");

        inicializado = true;
    }

    private static void carregarSomPorNome(String nome) {
        URL url = obterUrlDoResource(nome);
        if (url != null) {
            sonsCache.put(nome, url);
        } else {
            System.err.println("Som não encontrado: " + nome);
        }
    }

    private static void tocarSomUrlInterno(URL soundUrl) {
        if (soundUrl == null) return;

        String urlString = soundUrl.toExternalForm();

        if (urlString.toLowerCase().endsWith(".mp3")) {
            Media media = new Media(urlString);
            MediaPlayer player = new MediaPlayer(media);
            player.setVolume(volumeGeral);
            player.setCycleCount(1);
            player.setOnEndOfMedia(() -> {
                player.stop();
                player.dispose();
            });
            player.setOnError(() -> {
                System.err.println("Erro no MediaPlayer: " + player.getError());
                player.dispose();
            });

            playersAtivos.put(urlString, player);
            player.play();
        } else {
            AudioClip clip = new AudioClip(urlString);
            clip.setVolume(volumeGeral);
            clip.setCycleCount(1);
            clip.play();
        }
    }

    public static void tocarSomUrl(URL soundUrl) {
        if (soundUrl == null) return;
        if (!inicializado) inicializar();

        Platform.runLater(() -> tocarSomUrlInterno(soundUrl));
    }

    public static void tocarSom(TipoAlerta tipo) {
        if (!inicializado) inicializar();
        tocarSomPorNome(tipo.name().toLowerCase());
    }

    public static void tocarSomConfirmacao(TipoConfirmacao tipo) {
        if (!inicializado) inicializar();

        String nomeSom = "confirmacao_" + tipo.name().toLowerCase();
        if (!somDisponivel(nomeSom)) {
            nomeSom = "confirmation";
        }
        tocarSomPorNome(nomeSom);
    }

    private static void tocarSomPorNome(String nomeSom) {
        Platform.runLater(() -> {
            URL url = sonsCache.get(nomeSom);

            if (url == null) {
                url = obterUrlDoResource(nomeSom);
                if (url != null) {
                    sonsCache.put(nomeSom, url);
                }
            }

            if (url != null) {
                tocarSomUrlInterno(url);
                return;
            }

            if (nomeSom.startsWith("confirmacao_")) {
                URL fallback = sonsCache.get("confirmation");
                if (fallback != null) {
                    tocarSomUrlInterno(fallback);
                }
            }
        });
    }

    public static void tocarSomPersonalizado(String caminhoOuId) {
        if (!inicializado) inicializar();

        Platform.runLater(() -> {
            try {
                URL url = null;

                if (resourceAPI != null && resourceAPI.exists(caminhoOuId, ResourceType.SOUND)) {
                    url = resourceAPI.getSoundUrl(caminhoOuId);
                }

                if (url == null) {
                    url = new URL(caminhoOuId);
                }

                tocarSomUrlInterno(url);
            } catch (Exception e) {
                System.err.println("Falha ao tocar som personalizado: " + e.getMessage());
            }
        });
    }

    public static void setVolumeGeral(double volume) {
        volumeGeral = Math.max(0.0, Math.min(1.0, volume));
    }

    public static void pararTodos() {
        playersAtivos.values().forEach(player -> {
            try {
                if (player != null) {
                    player.stop();
                    player.dispose();
                }
            } catch (Exception ignored) {}
        });
        playersAtivos.clear();
    }

    public static void pararSom(String urlString) {
        MediaPlayer player = playersAtivos.get(urlString);
        if (player != null) {
            player.stop();
            player.dispose();
            playersAtivos.remove(urlString);
        }
    }

    public static boolean somDisponivel(String nome) {
        if (sonsCache.containsKey(nome)) return true;

        String resourceId = SOM_PARA_RESOURCE_ID.get(nome);
        return resourceAPI != null && resourceId != null && resourceAPI.exists(resourceId, ResourceType.SOUND);
    }

    public static void liberarSom(String nome) {
        sonsCache.remove(nome);
    }

    public static void reinicializar() {
        pararTodos();
        sonsCache.clear();
        inicializado = false;
        inicializar();
    }

    public static void diagnosticar() {
        System.out.println("ALERTA SONS - DIAGNÓSTICO");
        System.out.println("ResourceAPI: " + (resourceAPI != null ? "Vinculado" : "Não vinculado"));
        System.out.println("Inicializado: " + inicializado);
        System.out.println("Volume: " + (volumeGeral * 100) + "%");
        System.out.println("Sons em cache: " + sonsCache.size());
        System.out.println("Players ativos: " + playersAtivos.size());
    }
}