package com.ossobo.winterfx.AlertSystem.core;

import com.ossobo.winterfx.AlertSystem.core.position.AlertaPosicionador;
import com.ossobo.winterfx.AlertSystem.fx.AlertaConfirmacaoController;
import com.ossobo.winterfx.AlertSystem.fx.AlertaController;
import com.ossobo.winterfx.AlertSystem.model.*;
import com.ossobo.winterfx.AlertSystem.sound.AlertaSons;
import com.ossobo.winterfx.resources.api.ResourceAPI;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.ResourceType;
import com.ossobo.winterfx.view.ViewManager;
import com.ossobo.winterfx.view.loader.LoadedView;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.*;
import java.util.function.Consumer;

/**
 * Sistema central de gerenciamento de alertas.
 *
 * Carregamento de FXML delegado ao ViewManager.
 * AlertaSystem gerencia apenas: Stage, Overlay, Som, AutoClose.
 * Sem fallback — se falhar, explode na inicialização.
 *
 * v3.1 (24/04/2026):
 * - ✅ REMOVIDO: AlertaUI (fallback programático)
 * - ✅ REMOVIDO: criarAlertaFallback(), criarConfirmacaoFallback()
 * - ✅ Se ResourceAPI ou ViewManager falhar → exceção ruidosa
 */
public class AlertaSystem {
    private static AlertaSystem instance;
    private Stage primaryStage;
    private final Map<String, AlertInfo> alertasAtivos = new HashMap<>();
    private final Map<String, Timeline> timelines = new HashMap<>();

    private ResourceAPI resourceAPI;
    private ViewManager viewManager;

    private static final Duration TEMPO_SEMIMODAL = Duration.seconds(5);
    private static final Duration TEMPO_NAO_MODAL = Duration.seconds(3);

    private static final String ALERT_CRITICAL = "fx-alert-critical";
    private static final String ALERT_WARNING  = "fx-alert-warning";
    private static final String ALERT_INFO     = "fx-alert-info";
    private static final String ALERT_CONFIRM  = "fx-alert-confirm";

    private AlertaSystem() {
        inicializarSons();
    }

    public static synchronized AlertaSystem getInstance() {
        if (instance == null) {
            instance = new AlertaSystem();
        }
        return instance;
    }

    // ==================== INTEGRAÇÃO ====================

    public void setResourceAPI(ResourceAPI api) {
        this.resourceAPI = api;
        AlertaSons.setResourceAPI(api);
    }

    public void setViewManager(ViewManager vm) {
        this.viewManager = vm;
    }

    private ViewDescriptor obterDescriptor(String resourceId) {
        if (resourceAPI == null) {
            throw new IllegalStateException("ResourceAPI não vinculado ao AlertaSystem");
        }
        return resourceAPI.getAlertDescriptor(resourceId)
                .orElseThrow(() -> new IllegalStateException(
                        "ViewDescriptor não encontrado: " + resourceId));
    }

    private String resolverResourceId(Modalidade modalidade) {
        return switch (modalidade) {
            case MODAL      -> ALERT_CRITICAL;
            case SEMI_MODAL -> ALERT_WARNING;
            case NAO_MODAL  -> ALERT_INFO;
            default         -> ALERT_WARNING;
        };
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    public String criarAlerta(ModelAlert modelAlert) {
        return criarAlerta(
                modelAlert.getTitulo(),
                modelAlert.getDescricao(),
                modelAlert.getDetalhes(),
                modelAlert.getOrigem(),
                modelAlert.getOwnerNode(),
                modelAlert.getTipo(),
                modelAlert.getModalidade()
        );
    }

    public String criarAlerta(String titulo, String descricao, String detalhes,
                              String origem, Node ownerNode,
                              TipoAlerta tipo, Modalidade modalidade) {
        String id = UUID.randomUUID().toString();
        Platform.runLater(() ->
                processarCriacaoAlerta(id, titulo, descricao, detalhes,
                        origem, ownerNode, tipo, modalidade));
        return id;
    }

    // ===== CONVENIÊNCIA =====

    public String info(String titulo, String descricao) {
        return criarAlerta(ModelAlert.info(titulo, descricao));
    }

    public String info(String titulo, String descricao, String detalhes) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes).info().build());
    }

    public String warn(String titulo, String descricao) {
        return criarAlerta(ModelAlert.warn(titulo, descricao));
    }

    public String warn(String titulo, String descricao, String detalhes) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes).warn().build());
    }

    public String erro(String titulo, String descricao) {
        return criarAlerta(ModelAlert.erro(titulo, descricao));
    }

    public String erro(String titulo, String descricao, String detalhes) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes).erro().build());
    }

    public String critico(String titulo, String descricao) {
        return criarAlerta(ModelAlert.critico(titulo, descricao));
    }

    public String critico(String titulo, String descricao, String detalhes) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes).critico().build());
    }

    // ===== GERENCIAMENTO =====

    public void fecharAlerta(String id) {
        Platform.runLater(() -> processarFechamentoAlerta(id));
    }

    public void fecharTodosAlertas() {
        Platform.runLater(() ->
                new ArrayList<>(alertasAtivos.keySet()).forEach(this::processarFechamentoAlerta));
    }

    public int getQuantidadeAlertasAtivos() {
        return alertasAtivos.size();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    // ===== CONFIRMAÇÃO =====

    public String criarConfirmacao(String mensagem, String detalhes,
                                   String origem, Node ownerNode,
                                   TipoConfirmacao tipo, Consumer<Boolean> callbackResposta) {
        String id = UUID.randomUUID().toString();
        Platform.runLater(() ->
                processarCriacaoConfirmacao(id, mensagem, detalhes, origem, ownerNode, tipo, callbackResposta));
        return id;
    }

    public void confirmar(String mensagem, String titulo, Consumer<Boolean> callback) {
        criarConfirmacao(mensagem, null, titulo, null, TipoConfirmacao.PADRAO, callback);
    }

    public void confirmarComDetalhes(String mensagem, String detalhes, String titulo,
                                     TipoConfirmacao tipo, Consumer<Boolean> callback) {
        criarConfirmacao(mensagem, detalhes, titulo, null, tipo, callback);
    }

    // ==================== PROCESSAMENTO DE ALERTA ====================

    private void processarCriacaoAlerta(String id, String titulo, String descricao,
                                        String detalhes, String origem, Node ownerNode,
                                        TipoAlerta tipo, Modalidade modalidade) {
        String resourceId = resolverResourceId(modalidade);
        ViewDescriptor descriptor = obterDescriptor(resourceId);

        // ✅ ViewManager carrega FXML + aplica CSS + injeta DI + processa @FXImage
        LoadedView<AlertaController> loaded =
                viewManager.loadAlert(resourceId, AlertaController.class);

        Parent root = loaded.getRoot();
        AlertaController controller = loaded.getController();

        Stage alertStage = configurarStage(modalidade);
        alertStage.initStyle(StageStyle.UNDECORATED);
        alertStage.setTitle("Alerta - " + titulo);

        Scene scene = root.getScene() != null ? root.getScene() : new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        alertStage.setScene(scene);

        if (controller != null) {
            controller.configurarAlerta(titulo, descricao, origem, detalhes, tipo.name());
            controller.setAlertaId(id);
            controller.setAlertaStage(alertStage);
            controller.setPrimaryStage(primaryStage);
            controller.setOnCloseCallback(() -> fecharAlerta(id));

            if (modalidade != Modalidade.MODAL) {
                long autoClose = descriptor.getAutoCloseMillis();
                int segundos = autoClose > 0
                        ? (int) (autoClose / 1000)
                        : (int) (modalidade == Modalidade.NAO_MODAL
                                 ? TEMPO_NAO_MODAL.toSeconds()
                                 : TEMPO_SEMIMODAL.toSeconds());
                controller.configurarTemporizador(segundos);
            }
        }

        Pane overlay = null;
        if (modalidade != Modalidade.NAO_MODAL) {
            overlay = criarOverlay(ownerNode);
        }

        double largura = root.prefWidth(-1) > 0 ? root.prefWidth(-1) : 350;
        double altura = root.prefHeight(-1) > 0 ? root.prefHeight(-1) : 150;
        AlertaPosicionador.posicionar(alertStage, ownerNode, primaryStage, largura, altura);

        if (descriptor.getSoundUrl() != null) {
            AlertaSons.tocarSomUrl(descriptor.getSoundUrl());
        } else {
            AlertaSons.tocarSom(tipo);
        }

        alertStage.show();
        registrarAlerta(id, alertStage, tipo, modalidade, ownerNode, overlay);

        if (modalidade != Modalidade.MODAL && controller != null) {
            configurarAutoFechamento(id, alertStage, modalidade, controller);
        }
    }

    // ==================== PROCESSAMENTO DE CONFIRMAÇÃO ====================

    private void processarCriacaoConfirmacao(String id, String mensagem, String detalhes,
                                             String origem, Node ownerNode,
                                             TipoConfirmacao tipo, Consumer<Boolean> callback) {
        ViewDescriptor descriptor = obterDescriptor(ALERT_CONFIRM);

        LoadedView<AlertaConfirmacaoController> loaded =
                viewManager.loadAlert(ALERT_CONFIRM, AlertaConfirmacaoController.class);

        Parent root = loaded.getRoot();
        AlertaConfirmacaoController controller = loaded.getController();

        Stage confirmStage = new Stage();
        confirmStage.initStyle(StageStyle.UNDECORATED);
        confirmStage.setTitle("Confirmação - " + origem);
        confirmStage.initModality(Modality.APPLICATION_MODAL);
        if (primaryStage != null) confirmStage.initOwner(primaryStage);

        Scene scene = root.getScene() != null ? root.getScene() : new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        confirmStage.setScene(scene);

        Pane overlay = criarOverlay(ownerNode);

        if (controller != null) {
            controller.configurarConfirmacao(mensagem, detalhes, tipo.name());
            controller.setAlertaId(id);
            controller.setStage(confirmStage);
            controller.setPrimaryStage(primaryStage);

            controller.setCallbackResposta(resposta -> {
                confirmStage.close();
                removerOverlay(overlay);
                alertasAtivos.remove(id);
                if (callback != null) callback.accept(resposta);
                if (primaryStage != null) primaryStage.requestFocus();
            });
        }

        double largura = root.prefWidth(-1) > 0 ? root.prefWidth(-1) : 400;
        double altura = root.prefHeight(-1) > 0 ? root.prefHeight(-1) : 220;
        AlertaPosicionador.posicionar(confirmStage, ownerNode, primaryStage, largura, altura);

        if (descriptor.getSoundUrl() != null) {
            AlertaSons.tocarSomUrl(descriptor.getSoundUrl());
        } else {
            AlertaSons.tocarSomConfirmacao(tipo);
        }

        AlertInfo info = new AlertInfo(id, confirmStage, TipoAlerta.INFO, Modalidade.MODAL, ownerNode, overlay);
        alertasAtivos.put(id, info);

        confirmStage.setOnHidden(e -> {
            removerOverlay(overlay);
            alertasAtivos.remove(id);
            if (primaryStage != null) primaryStage.requestFocus();
        });

        confirmStage.show();
    }

    // ==================== OVERLAY ====================

    private Pane criarOverlay(Node ownerNode) {
        Pane overlay = new Pane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
        overlay.setPickOnBounds(true);

        Node rootNode = AlertaPosicionador.obterRootNode(ownerNode, primaryStage);
        if (rootNode instanceof Pane pane) {
            pane.getChildren().add(overlay);
            AlertaPosicionador.configurarBindings(overlay, pane);
        }
        return overlay;
    }

    private void removerOverlay(Pane overlay) {
        if (overlay != null && overlay.getParent() instanceof Pane pane) {
            pane.getChildren().remove(overlay);
            pane.requestLayout();
        }
    }

    // ==================== STAGE / AUTO-CLOSE ====================

    private Stage configurarStage(Modalidade modalidade) {
        Stage stage = new Stage();
        switch (modalidade) {
            case MODAL -> stage.initModality(Modality.APPLICATION_MODAL);
            case SEMI_MODAL -> {
                stage.initModality(Modality.WINDOW_MODAL);
                if (primaryStage != null) stage.initOwner(primaryStage);
            }
            case NAO_MODAL -> stage.initModality(Modality.NONE);
        }
        return stage;
    }

    private void configurarAutoFechamento(String id, Stage stage, Modalidade modalidade,
                                          AlertaController controller) {
        Duration tempo = modalidade == Modalidade.NAO_MODAL ? TEMPO_NAO_MODAL : TEMPO_SEMIMODAL;
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    if (controller != null) controller.atualizarTemporizador();
                }),
                new KeyFrame(tempo, e -> fecharAlerta(id))
        );
        timeline.setCycleCount((int) tempo.toSeconds());
        timeline.play();
        timelines.put(id, timeline);
    }

    private void registrarAlerta(String id, Stage stage, TipoAlerta tipo,
                                 Modalidade modalidade, Node ownerNode, Pane overlay) {
        alertasAtivos.put(id, new AlertInfo(id, stage, tipo, modalidade, ownerNode, overlay));
    }

    // ==================== FECHAMENTO ====================

    private void processarFechamentoAlerta(String id) {
        AlertInfo info = alertasAtivos.get(id);
        if (info != null) {
            Timeline timeline = timelines.remove(id);
            if (timeline != null) timeline.stop();
            removerOverlay(info.overlayBlock);
            if (info.stage != null && info.stage.isShowing()) {
                try {
                    if (info.stage.getScene() != null) {
                        Parent root = info.stage.getScene().getRoot();
                        if (root != null) {
                            FadeTransition fade = new FadeTransition(Duration.millis(200), root);
                            fade.setFromValue(1.0);
                            fade.setToValue(0.0);
                            fade.setOnFinished(e -> {
                                info.stage.close();
                                alertasAtivos.remove(id);
                            });
                            fade.play();
                            return;
                        }
                    }
                } catch (Exception e) {
                    // ignora
                }
                info.stage.close();
                alertasAtivos.remove(id);
            }
        }
    }

    private void inicializarSons() {
        AlertaSons.inicializar();
    }

    // ==================== UTILITÁRIOS ====================

    public boolean isAlertaAtivo(String id) {
        return alertasAtivos.containsKey(id);
    }

    public List<String> listarAlertasAtivos() {
        List<String> lista = new ArrayList<>();
        for (AlertInfo info : alertasAtivos.values()) {
            lista.add(String.format("[%s] %s - %s",
                    info.tipo, info.modalidade, info.id.substring(0, 8)));
        }
        return lista;
    }

    public void setVolume(double volume) {
        AlertaSons.setVolumeGeral(volume);
    }

    public void limparTodosOverlays() {
        for (AlertInfo info : alertasAtivos.values()) {
            removerOverlay(info.overlayBlock);
        }
    }

    // ==================== DIAGNÓSTICO ====================

    public void diagnosticarSistema() {
        System.out.println("\n🔍 ALERTA SYSTEM v3.1 - DIAGNÓSTICO");
        System.out.println("=".repeat(50));
        System.out.println("• ResourceAPI: " + (resourceAPI != null ? "✅" : "❌"));
        System.out.println("• ViewManager: " + (viewManager != null ? "✅" : "❌"));
        System.out.println("• Alertas ativos: " + alertasAtivos.size());
        System.out.println("• PrimaryStage: " + (primaryStage != null ? "✅" : "❌"));

        if (resourceAPI != null) {
            System.out.println("\n📋 ALERTAS NO RESOURCE API:");
            resourceAPI.listIdsByType(ResourceType.ALERT).forEach(id ->
                    System.out.println("  • " + id + " → " + (resourceAPI.exists(id) ? "✅" : "❌")));
        }
        System.out.println("=".repeat(50));
    }
}