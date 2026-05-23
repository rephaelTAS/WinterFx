package com.ossobo.winterfx.AlertSystem.fx;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;

public class AlertaController {

    @FXML private Label lblTitulo;
    @FXML private Label lblDescricao;
    @FXML private Label lblOrigem;
    @FXML private Label lblTempo;
    @FXML private Button btnOk;
    @FXML private Button btnDetalhes;
    @FXML private ProgressBar progressoTempo;
    @FXML private VBox containerPrincipal;

    private Stage stage;
    private Stage primaryStage;
    private Runnable onCloseCallback;
    private String detalhesTexto;
    private String alertaId;
    private int tempoRestante;
    private boolean fechando = false; // Flag para evitar duplo fechamento

    // ===== MÉTODOS DE INICIALIZAÇÃO =====

    @FXML
    public void initialize() {

        // Configurar ações dos botões PROGRAMATICAMENTE
        if (btnOk != null) {
            btnOk.setOnAction(e -> {
                onOkAction();
            });
        }

        if (btnDetalhes != null) {
            btnDetalhes.setOnAction(e -> {
                onDetalhesAction();
            });
        }

        // Adicionar listener para fechar com ESC
        if (containerPrincipal != null && containerPrincipal.getScene() != null) {
            containerPrincipal.getScene().setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case ESCAPE:
                        onOkAction();
                        break;
                    case ENTER:
                        onOkAction();
                        break;
                }
            });
        }
    }

    // ===== MÉTODOS PÚBLICOS PARA O ALERTASYSTEM =====

    public void configurarAlerta(String titulo, String descricao, String origem,
                                 String detalhes, String tipoAlerta) {

        if (lblTitulo != null) {
            lblTitulo.setText(titulo);
        }

        if (lblDescricao != null) {
            lblDescricao.setText(descricao);
        }

        if (lblOrigem != null) {
            lblOrigem.setText("Origem: " + origem);
        }

        detalhesTexto = detalhes;

        aplicarEstiloTipo(tipoAlerta);
        configurarVisibilidadeDetalhes(detalhes);
    }

    public void setAlertaStage(Stage stage) {
        this.stage = stage;

        // Quando o stage for fechado, garantir callback
        if (stage != null) {
            stage.setOnCloseRequest(e -> {
                if (!fechando && onCloseCallback != null) {
                    fechando = true;
                    onCloseCallback.run();
                }
            });

            // Também configurar quando o stage for escondido
            stage.setOnHidden(e -> {
                if (!fechando && onCloseCallback != null) {
                    fechando = true;
                    onCloseCallback.run();
                }
            });
        }
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setAlertaId(String id) {
        this.alertaId = id;
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    public void configurarTemporizador(int segundos) {
        this.tempoRestante = segundos;

        if (progressoTempo != null && lblTempo != null) {
            progressoTempo.setProgress(1.0);
            atualizarDisplayTempo();
        }
    }

    public void atualizarTemporizador() {
        if (tempoRestante > 0) {
            tempoRestante--;
            atualizarDisplayTempo();

            if (progressoTempo != null) {
                double progresso = (double) tempoRestante / (tempoRestante + 1);
                progressoTempo.setProgress(progresso);
            }
        }
    }

    // ===== MÉTODOS DE AÇÃO (chamados pelos botões) =====

    @FXML
    private void onOkAction() {


        fecharAlerta();
    }

    @FXML
    private void onDetalhesAction() {
        mostrarDetalhes();
    }

    // ===== MÉTODOS PRIVADOS =====

    private void aplicarEstiloTipo(String tipo) {
        if (containerPrincipal != null) {

            // Limpar estilos anteriores
            containerPrincipal.getStyleClass().removeIf(s -> s.startsWith("alerta-"));

            // Adicionar novo estilo
            containerPrincipal.getStyleClass().add("alerta-" + tipo.toLowerCase());

            // Adicionar ícone ao título
            if (lblTitulo != null) {
                lblTitulo.getStyleClass().removeIf(s -> s.startsWith("icone-"));
                lblTitulo.getStyleClass().add("icone-" + tipo.toLowerCase());
            }


        }
    }

    private void configurarVisibilidadeDetalhes(String detalhes) {
        if (btnDetalhes != null) {
            boolean temDetalhes = detalhes != null && !detalhes.isEmpty();
            btnDetalhes.setVisible(temDetalhes);
            btnDetalhes.setManaged(temDetalhes);
        }
    }

    private void atualizarDisplayTempo() {
        if (lblTempo != null) {
            lblTempo.setText(tempoRestante + "s");
        }
    }

    private void mostrarDetalhes() {
        try {

            // Usar caminho absoluto para o FXML
            String fxmlPath = "/packt/frameworks/nexusfx/AlertSystem/fx/fxml/alerta-detalhes.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));


            Parent root = loader.load();

            AlertaDetalhesController controller = loader.getController();

            if (controller != null) {
                controller.setDetalhes(detalhesTexto);
     ;
            }

            Stage detalhesStage = new Stage();
            detalhesStage.initStyle(StageStyle.UTILITY);
            detalhesStage.setTitle("Detalhes do Alerta - " + (lblTitulo != null ? lblTitulo.getText() : "Sem título"));

            // Configurar modalidade
            if (primaryStage != null) {
                detalhesStage.initOwner(primaryStage);
                detalhesStage.initModality(Modality.WINDOW_MODAL);
            } else if (stage != null) {
                detalhesStage.initOwner(stage);
                detalhesStage.initModality(Modality.WINDOW_MODAL);

            }

            Scene scene = new Scene(root, 500, 400);
            detalhesStage.setScene(scene);

            // Centralizar na tela
            detalhesStage.centerOnScreen();
            detalhesStage.show();



        } catch (IOException e) {

            TextArea area = new TextArea(detalhesTexto != null ? detalhesTexto : "Nenhum detalhe disponível.");
            area.setEditable(false);
            area.setWrapText(true);
            area.setPrefRowCount(20);

            Stage fallbackStage = new Stage();
            fallbackStage.setTitle("Detalhes do Alerta - " + (lblTitulo != null ? lblTitulo.getText() : "Sem título"));

            if (stage != null) {
                fallbackStage.initOwner(stage);
            }

            fallbackStage.setScene(new Scene(new javafx.scene.layout.StackPane(area), 500, 400));
            fallbackStage.show();

        }
    }

    private void fecharAlerta() {


        fechando = true; // Marcar como em processo de fechamento

        // Executar callback primeiro para remover overlay e limpar registros
        if (onCloseCallback != null) {

                onCloseCallback.run();

        }

        // Depois fechar o stage com animação
        if (stage != null) {
            animarFechamento();
        } else {
            fechando = false; // Resetar flag se não puder fechar
        }
    }

    private void animarFechamento() {

        if (containerPrincipal != null) {

            FadeTransition fade = new FadeTransition(Duration.millis(200), containerPrincipal);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);

            fade.setOnFinished(e -> {
                if (stage != null) {
                    stage.close();

                }
            });

            fade.play();
        } else {
            if (stage != null) {
                stage.close();
            }
        }
    }

    /** Método para forçar fechamento sem animação (emergência) */
    public void fecharImediatamente() {

        fechando = true;

        // Executar callback
        if (onCloseCallback != null) {
            try {
                onCloseCallback.run();
            } catch (Exception e) {
            }
        }

        // Fechar stage
        if (stage != null) {
         stage.close();

        }
    }

    /** Verifica se o alerta está em processo de fechamento */
    public boolean isFechando() {
        return fechando;
    }

    /** Obtém o ID do alerta */
    public String getAlertaId() {
        return alertaId;
    }

    /** Obtém o stage do alerta */
    public Stage getAlertaStage() {
        return stage;
    }
}
