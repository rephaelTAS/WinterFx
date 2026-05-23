package com.ossobo.winterfx.AlertSystem.core.ui;

import com.ossobo.winterfx.AlertSystem.core.AlertaSystem;
import com.ossobo.winterfx.AlertSystem.core.animation.AlertaAnimador;
import com.ossobo.winterfx.AlertSystem.core.position.AlertaPosicionador;
import com.ossobo.winterfx.AlertSystem.model.TipoAlerta;
import com.ossobo.winterfx.AlertSystem.model.TipoConfirmacao;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Responsável pela criação de componentes visuais dos alertas
 * Mantém coesão forte - apenas interface gráfica
 */
public class AlertaUI {

    /** Cria o conteúdo visual do alerta - ATUALIZADO para usar Label */
    public static VBox criarConteudo(String titulo, String descricao, String detalhes,
                                     String origem, TipoAlerta tipo) {
        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setPrefSize(350, 150);
        container.setEffect(new DropShadow(10, Color.BLACK));

        aplicarEstiloTipo(container, tipo);
        adicionarComponentes(container, titulo, descricao, origem, detalhes);

        return container;
    }

    /** Aplica estilo baseado no tipo de alerta */
    private static void aplicarEstiloTipo(VBox container, TipoAlerta tipo) {
        Color corBase = switch (tipo) {
            case INFO -> javafx.scene.paint.Color.DODGERBLUE;
            case WARN -> javafx.scene.paint.Color.ORANGE;
            case ERRO -> javafx.scene.paint.Color.RED;
            case CRITICAL -> javafx.scene.paint.Color.DARKRED;
        };
        Color corBorda = corBase.darker();

        String estilo = String.format(
                "-fx-background-color: linear-gradient(to bottom, %s, derive(%s, -20%%)); " +
                        "-fx-border-color: %s; -fx-border-width: 2; " +
                        "-fx-border-radius: 10; -fx-background-radius: 10;",
                toHex(corBase), toHex(corBase), toHex(corBorda)
        );
        container.setStyle(estilo);
    }

    /** Adiciona componentes visuais ao container - ATUALIZADO */
    private static void adicionarComponentes(VBox container, String titulo,
                                             String descricao, String origem, String detalhes) {
        Label lblTitulo = criarLabel(titulo,
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 0 0 5 0;");

        // MUDADO: TextArea → Label
        Label lblDescricao = criarLabel(descricao,
                "-fx-font-size: 13px; -fx-text-fill: white; -fx-wrap-text: true; -fx-background-color: transparent; " +
                        "-fx-padding: 8px; -fx-max-width: 320px;");

        Label lblOrigem = criarLabel("Origem: " + origem,
                "-fx-font-size: 11px; -fx-text-fill: #DDDDDD; -fx-padding: 5 0 0 0;");

        HBox botoes = criarBotoesAcao(detalhes);

        container.getChildren().addAll(lblTitulo, lblDescricao, lblOrigem, botoes);
    }

    /** Cria label estilizada */
    private static Label criarLabel(String texto, String estilo) {
        Label label = new Label(texto);
        label.setStyle(estilo);
        label.setWrapText(true); // Importante para quebrar linhas
        return label;
    }

    /** Cria área de texto para descrição - MANTIDO apenas para compatibilidade */
    private static TextArea criarTextArea(String texto) {
        TextArea area = new TextArea(texto);
        area.setEditable(false);
        area.setWrapText(true);
        area.setStyle("-fx-control-inner-background: transparent; -fx-text-fill: white;");
        area.setPrefHeight(60);
        return area;
    }

    /** Cria barra de botões de ação */
    private static HBox criarBotoesAcao(String detalhes) {
        HBox botoes = new HBox(10);
        botoes.setAlignment(Pos.CENTER_RIGHT);

        if (detalhes != null && !detalhes.isEmpty()) {
            Button btnDetalhes = criarBotao("Detalhes", e -> mostrarDetalhes(detalhes));
            botoes.getChildren().add(btnDetalhes);
        }

        Button btnOk = criarBotao("OK", e -> fecharAlertaAtual(botoes));
        botoes.getChildren().add(btnOk);

        return botoes;
    }

    /** Cria botão estilizado */
    private static Button criarBotao(String texto, javafx.event.EventHandler<javafx.event.ActionEvent> acao) {
        Button btn = new Button(texto);
        btn.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; " +
                "-fx-padding: 5 15 5 15; -fx-border-radius: 3; -fx-background-radius: 3;");
        btn.setOnAction(acao);
        return btn;
    }

    /** Cria overlay de bloqueio */
    public static Pane criarOverlayBloqueio(Node ownerNode, Stage primaryStage) {
        VBox overlay = new VBox();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        overlay.setPickOnBounds(true);

        Node root = AlertaPosicionador.obterRootNode(ownerNode, primaryStage);
        if (root instanceof Pane) {
            Pane container = (Pane) root;

            boolean overlayExistente = container.getChildren().stream()
                    .anyMatch(node -> node instanceof Pane &&
                            node.getId() != null &&
                            node.getId().startsWith("alerta-overlay-"));

            if (!overlayExistente) {
                container.getChildren().add(overlay);
                AlertaPosicionador.configurarBindings(overlay, container);
                overlay.setId("alerta-overlay-" + System.currentTimeMillis());
            } else {
                return (Pane) container.getChildren().stream()
                        .filter(node -> node instanceof Pane &&
                                node.getId() != null &&
                                node.getId().startsWith("alerta-overlay-"))
                        .findFirst()
                        .orElse(overlay);
            }
        }

        return overlay;
    }

    /** Método para remover overlay específico */
    public static void removerOverlayBloqueio(Pane overlay) {
        if (overlay != null) {

            Parent parent = overlay.getParent();
            if (parent instanceof Pane) {
                Pane container = (Pane) parent;

                if (container.getChildren().contains(overlay)) {
                    container.getChildren().remove(overlay);
                    System.out.println("✅ Overlay removido do container com sucesso");
                    container.requestLayout();
                }
            }
        }
    }

    /** Remove overlay associado ao stage */
    public static void removerOverlayDoStage(Stage stage) {
        if (stage != null) {

            if (stage.getOwner() != null) {
                Window owner = stage.getOwner();
                if (owner instanceof Stage) {
                    Stage ownerStage = (Stage) owner;
                    Scene ownerScene = ownerStage.getScene();
                    if (ownerScene != null && ownerScene.getRoot() instanceof Pane) {
                        Pane ownerRoot = (Pane) ownerScene.getRoot();
                        int removidos = removerOverlaysDoPane(ownerRoot);
                    }
                }
            } else if (AlertaSystem.getInstance().getPrimaryStage() != null) {
                Stage primaryStage = AlertaSystem.getInstance().getPrimaryStage();
                Scene primaryScene = primaryStage.getScene();
                if (primaryScene != null && primaryScene.getRoot() instanceof Pane) {
                    Pane primaryRoot = (Pane) primaryScene.getRoot();
                    int removidos = removerOverlaysDoPane(primaryRoot);
                }
            }
        }
    }

    /** Remove overlays de um pane específico */
    private static int removerOverlaysDoPane(Pane pane) {
        int contador = 0;
        java.util.List<Node> nodesParaRemover = new java.util.ArrayList<>();

        for (Node node : pane.getChildren()) {
            if (node instanceof Pane && node.getId() != null &&
                    node.getId().startsWith("alerta-overlay-")) {
                nodesParaRemover.add(node);
                contador++;
            }
        }

        pane.getChildren().removeAll(nodesParaRemover);

        if (contador > 0) {
            pane.requestLayout();
        }

        return contador;
    }

    /** Fecha o alerta atual */
    private static void fecharAlertaAtual(HBox botoes) {
        Scene cena = botoes.getScene();
        if (cena != null) {
            Window janela = cena.getWindow();
            if (janela instanceof Stage) {
                Stage stage = (Stage) janela;
                Parent root = cena.getRoot();

                System.out.println("🔄 Fechando alerta atual...");
                removerOverlayDoStage(stage);

                if (root instanceof VBox) {
                    AlertaAnimador.animarSaida((VBox) root, stage);
                } else {
                    animarFechamentoGenerico(root, stage);
                }
            }
        }
    }

    /** Mostra janela de detalhes */
    private static void mostrarDetalhes(String detalhes) {
        TextArea area = new TextArea(detalhes);
        area.setEditable(false);
        area.setWrapText(true);

        Stage stage = new Stage();
        stage.setScene(new Scene(new StackPane(area), 400, 300));
        stage.setTitle("Detalhes do Alerta");
        stage.show();
    }

    /** Anima fechamento genérico para qualquer tipo de Parent */
    private static void animarFechamentoGenerico(Parent root, Stage stage) {
        FadeTransition fade = new FadeTransition(Duration.millis(200), root);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> {
            stage.close();
        });
        fade.play();
    }

    /** Converte Color para hexadecimal */
    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    /** Método utilitário para diagnosticar overlays */
    public static void diagnosticarOverlays(Stage stage) {
        if (stage != null) {

            if (stage.getOwner() instanceof Stage) {
                Stage ownerStage = (Stage) stage.getOwner();
                Scene ownerScene = ownerStage.getScene();
                if (ownerScene != null && ownerScene.getRoot() instanceof Pane) {
                    Pane root = (Pane) ownerScene.getRoot();
                    System.out.println("  Root pane: " + root.getClass().getName());
                    System.out.println("  Número de children: " + root.getChildren().size());

                    int overlayCount = 0;
                    for (Node node : root.getChildren()) {
                        if (node instanceof Pane) {
                            System.out.println("    - " + node.getClass().getName() +
                                    " (ID: " + node.getId() + ")");
                            if (node.getId() != null && node.getId().startsWith("alerta-overlay-")) {
                                overlayCount++;
                            }
                        }
                    }
                }
            }
        }
    }

    /** Cria conteúdo para confirmação programática (fallback) */
    public static VBox criarConteudoConfirmacao(String mensagem, String detalhes,
                                                TipoConfirmacao tipo,
                                                Consumer<Boolean> callbackResposta) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setPrefSize(400, 220);
        container.getStyleClass().add("alerta-confirmacao");

        // Aplicar estilo baseado no tipo
        aplicarEstiloConfirmacao(container, tipo);

        // Ícone
        Label icone = new Label("?");
        icone.getStyleClass().add("icone-confirmacao");
        HBox iconeContainer = new HBox(icone);
        iconeContainer.setAlignment(Pos.CENTER);

        // Mensagem
        Label lblMensagem = new Label(mensagem);
        lblMensagem.getStyleClass().add("mensagem-confirmacao");
        lblMensagem.setWrapText(true);
        lblMensagem.setMaxWidth(350);

        // Detalhes (se houver)
        TextArea txtDetalhes = null;
        if (detalhes != null && !detalhes.isEmpty()) {
            txtDetalhes = new TextArea(detalhes);
            txtDetalhes.getStyleClass().add("detalhes-confirmacao");
            txtDetalhes.setEditable(false);
            txtDetalhes.setWrapText(true);
            txtDetalhes.setPrefRowCount(3);
            txtDetalhes.setVisible(true);
            txtDetalhes.setManaged(true);
        }

        // Botões
        HBox botoes = new HBox(15);
        botoes.setAlignment(Pos.CENTER);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("botao-cancelar");
        btnCancelar.setOnAction(e -> {
            if (callbackResposta != null) {
                callbackResposta.accept(false);
            }
            fecharConfirmacao(container);
        });

        Button btnConfirmar = new Button("Confirmar");
        btnConfirmar.getStyleClass().add("botao-confirmar");
        btnConfirmar.setOnAction(e -> {
            if (callbackResposta != null) {
                callbackResposta.accept(true);
            }
            fecharConfirmacao(container);
        });

        botoes.getChildren().addAll(btnCancelar, btnConfirmar);

        // Adicionar componentes ao container
        if (txtDetalhes != null) {
            container.getChildren().addAll(iconeContainer, lblMensagem, txtDetalhes, botoes);
        } else {
            container.getChildren().addAll(iconeContainer, lblMensagem, botoes);
        }

        // Configurar teclas de atalho
        container.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER:
                    if (callbackResposta != null) callbackResposta.accept(true);
                    fecharConfirmacao(container);
                    break;
                case ESCAPE:
                    if (callbackResposta != null) callbackResposta.accept(false);
                    fecharConfirmacao(container);
                    break;
                case Y:
                    if (!e.isControlDown()) {
                        if (callbackResposta != null) callbackResposta.accept(true);
                        fecharConfirmacao(container);
                    }
                    break;
                case N:
                    if (!e.isControlDown()) {
                        if (callbackResposta != null) callbackResposta.accept(false);
                        fecharConfirmacao(container);
                    }
                    break;
            }
        });

        return container;
    }

    /** Aplica estilo baseado no tipo de confirmação */
    private static void aplicarEstiloConfirmacao(VBox container, TipoConfirmacao tipo) {
        // Limpar estilos anteriores
        container.getStyleClass().removeIf(s ->
                s.equals("perigo") || s.equals("aviso") ||
                        s.equals("info") || s.equals("sucesso") || s.equals("padrao"));

        // Adicionar classe do tipo
        container.getStyleClass().add(tipo.name().toLowerCase());

        // Aplicar estilo base
        String estiloBase = "-fx-background-color: linear-gradient(to bottom, #f8f9fa, #e9ecef); " +
                "-fx-border-color: #6c757d; -fx-border-width: 2; " +
                "-fx-border-radius: 10; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0.2, 0, 5);";
        container.setStyle(estiloBase);
    }

    /** Fecha uma janela de confirmação */
    private static void fecharConfirmacao(VBox container) {
        Scene cena = container.getScene();
        if (cena != null) {
            Window janela = cena.getWindow();
            if (janela instanceof Stage) {
                Stage stage = (Stage) janela;

                // Animar fechamento
                FadeTransition fade = new FadeTransition(Duration.millis(150), container);
                fade.setFromValue(1.0);
                fade.setToValue(0.0);
                fade.setOnFinished(e -> stage.close());
                fade.play();
            }
        }
    }

    /** Cria botão personalizado para confirmação */
    public static Button criarBotaoConfirmacao(String texto, String corHex,
                                               javafx.event.EventHandler<javafx.event.ActionEvent> acao) {
        Button btn = new Button(texto);
        btn.setStyle("-fx-background-color: " + corHex + "; " +
                "-fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-font-size: 14px; -fx-padding: 8px 20px; " +
                "-fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-cursor: hand;");
        btn.setOnAction(acao);

        // Efeitos hover
        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-background-color: " + escurecerCor(corHex) + "; " +
                    "-fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-font-size: 14px; -fx-padding: 8px 20px; " +
                    "-fx-border-radius: 6; -fx-background-radius: 6; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, " + corHex + "80, 8, 0.3, 0, 3);");
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-background-color: " + corHex + "; " +
                    "-fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-font-size: 14px; -fx-padding: 8px 20px; " +
                    "-fx-border-radius: 6; -fx-background-radius: 6; " +
                    "-fx-cursor: hand;");
        });

        return btn;
    }

    /** Escurece uma cor hexadecimal para efeitos hover */
    private static String escurecerCor(String corHex) {
        try {
            // Remove o # se existir
            String hex = corHex.startsWith("#") ? corHex.substring(1) : corHex;

            // Converte para valores RGB
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            // Escurece em 20%
            r = Math.max(0, (int)(r * 0.8));
            g = Math.max(0, (int)(g * 0.8));
            b = Math.max(0, (int)(b * 0.8));

            // Converte de volta para hexadecimal
            return String.format("#%02X%02X%02X", r, g, b);
        } catch (Exception e) {
            // Se houver erro, retorna a cor original
            return corHex;
        }
    }
}
