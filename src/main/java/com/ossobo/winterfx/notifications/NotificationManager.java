package com.ossobo.winterfx.notifications;

import java.util.function.Consumer;
// NotificationManager.java v6.0 - 2026-06-14
// API completa para notificações com suporte a detalhes, gerenciamento de alertas e confirmações.
//
// Vantagens v6.0:
//   - ✅ Suporte a detalhes (métodos com 3 parâmetros)
//   - ✅ Retorna ID do alerta (para fechamento manual)
//   - ✅ Temporizador automático: INFO (5s), SUCCESS (3s), WARNING (5s)
//   - ✅ ERROR, CRITICAL, CONFIRMATION ficam até fechar (usuário fecha)
//   - ✅ Gerenciamento: fecharAlerta(id), fecharTodosAlertas(), getQuantidadeAlertasAtivos()
//   - ✅ Confirmações: confirmar(), confirmarComDetalhes() com callback
//   - ✅ UNDECORATED para todas notificações
//   - ✅ Fallback nativo se view não registrada
//   - ✅ Logging robusto (info, warning, severe)
//
// @version 6.0 - API completa com detalhes, gerenciamento e confirmações


import com.ossobo.winterfx.notifications.enums.NotificationType;
import com.ossobo.winterfx.notifications.enums.AlertType;
import com.ossobo.winterfx.notifications.registry.NotificationViewRegistrar;
import com.ossobo.winterfx.notifications.resolver.NotificationViewResolver;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.StageManager;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔔 NotificationManager v6.0
 *
 * API completa para notificações com suporte a detalhes, gerenciamento e confirmações.
 *
 * <p><b>Temporizador automático:</b></p>
 * <ul>
 *   <li>{@code info()}: 5 segundos</li>
 *   <li>{@code success()}: 3 segundos</li>
 *   <li>{@code warn()}: 5 segundos</li>
 *   <li>{@code erro()}: fica até fechar (usuário fecha)</li>
 *   <li>{@code critico()}: fica até fechar (usuário fecha) + retorna ID</li>
 * </ul>
 *
 * <p><b>Métodos públicos:</b></p>
 * <ul>
 *   <li>{@link #info(String, String)}: notificação informativa (sem detalhes)</li>
 *   <li>{@link #info(String, String, String)}: notificação informativa (com detalhes)</li>
 *   <li>{@link #success(String, String)}: notificação de sucesso</li>
 *   <li>{@link #warn(String, String)}: notificação de aviso (sem detalhes)</li>
 *   <li>{@link #warn(String, String, String)}: notificação de aviso (com detalhes)</li>
 *   <li>{@link #erro(String, String)}: notificação de erro (sem detalhes)</li>
 *   <li>{@link #erro(String, String, String)}: notificação de erro (com detalhes)</li>
 *   <li>{@link #critico(String, String)}: notificação crítica (sem detalhes)</li>
 *   <li>{@link #critico(String, String, String)}: notificação crítica (com detalhes)</li>
 *   <li>{@link #fecharAlerta(String)}: fecha alerta específico por ID</li>
 *   <li>{@link #fecharTodosAlertas()}: fecha todos os alertas ativos</li>
 *   <li>{@link #getQuantidadeAlertasAtivos()}: retorna quantidade de alertas ativos</li>
 *   <li>{@link #confirmar(String, String, Consumer)}: confirmação Sim/Não com callback</li>
 *   <li>{@link #confirmarComDetalhes(String, String, String, TipoConfirmacao, Consumer)}: confirmação com detalhes</li>
 * </ul>
 *
 * @version 6.0 - API completa com detalhes, gerenciamento e confirmações
 */
public class NotificationManager {

    // Alertas ativos (ID → Stage)
    private final Map<String, javafx.stage.Stage> alertasAtivos = new ConcurrentHashMap<>();

    private StageManager stageManager;
    private final ResourceRegistry resourceRegistry;

    /**
     * Construtor com ResourceRegistry.
     *
     * @param resourceRegistry Registro de recursos (views, descriptors)
     */
    public NotificationManager(ResourceRegistry resourceRegistry) {
        this.resourceRegistry = resourceRegistry;
        NotificationViewRegistrar.registerAll(resourceRegistry);
    }

    /**
     * Configura StageManager (inicialização lazy).
     *
     * @param stageManager Gerente de stages
     */
    public void setStageManager(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    // =============================================
    // API PÚBLICA - NOTIFICAÇÕES
    // =============================================

    /**
     * Notificação informativa — some em 5 segundos (sem detalhes).
     *
     * @param titulo Título da notificação
     * @param descricao Descrição detalhada
     * @return ID do alerta (para fechamento manual se necessário)
     */
    public String info(String titulo, String descricao) {
        return criarAlerta(ModelAlert.info(titulo, descricao));
    }

    /**
     * Notificação informativa — some em 5 segundos (com detalhes).
     *
     * @param titulo Título da notificação
     * @param descricao Descrição detalhada
     * @param detalhes Detalhes adicionais (opcional)
     * @return ID do alerta (para fechamento manual se necessário)
     */
    public String info(String titulo, String descricao, String detalhes) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes).info().build());
    }

    /**
     * Notificação de sucesso — some em 3 segundos.
     *
     * @param titulo Título da notificação
     * @param descricao Descrição detalhada
     * @return ID do alerta (para fechamento manual se necessário)
     */
    public String success(String titulo, String descricao) {
        return criarAlerta(ModelAlert.success(titulo, descricao));
    }

    /**
     * Notificação de aviso — some em 5 segundos (sem detalhes).
     *
     * @param titulo Título da notificação
     * @param descricao Descrição detalhada
     * @return ID do alerta (para fechamento manual se necessário)
     */
    public String warn(String titulo, String descricao) {
        return criarAlerta(ModelAlert.warn(titulo, descricao));
    }

    /**
     * Notificação de aviso — some em 5 segundos (com detalhes).
     *
     * @param titulo Título da notificação
     * @param descricao Descrição detalhada
     * @param detalhes Detalhes adicionais (opcional)
     * @return ID do alerta (para fechamento manual se necessário)
     */
    public String warn(String titulo, String descricao, String detalhes) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes).warn().build());
    }

    /**
     * Notificação de erro — NÃO some (precisa fechar manualmente, sem detalhes).
     *
     * @param titulo Título da notificação
     * @param descricao Descrição detalhada
     * @return ID do alerta (para fechamento manual)
     */
    public String erro(String titulo, String descricao) {
        return criarAlerta(ModelAlert.erro(titulo, descricao));
    }

    /**
     * Notificação de erro — NÃO some (precisa fechar manualmente, com detalhes).
     *
     * @param titulo Título da notificação
     * @param descricao Descrição detalhada
     * @param detalhes Detalhes adicionais (opcional)
     * @return ID do alerta (para fechamento manual)
     */
    public String erro(String titulo, String descricao, String detalhes) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes).erro().build());
    }

    /**
     * Notificação crítica — NÃO some (precisa fechar, sem detalhes).
     *
     * @param titulo Título da notificação
     * @param descricao Descrição detalhada
     * @return ID do alerta (para fechamento manual)
     */
    public String critico(String titulo, String descricao) {
        return criarAlerta(ModelAlert.critico(titulo, descricao));
    }

    /**
     * Notificação crítica — NÃO some (precisa fechar, com detalhes).
     *
     * @param titulo Título da notificação
     * @param descricao Descrição detalhada
     * @param detalhes Detalhes adicionais (opcional)
     * @return ID do alerta (para fechamento manual)
     */
    public String critico(String titulo, String descricao, String detalhes) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes).critico().build());
    }

    // =============================================
    // API PÚBLICA - GERENCIAMENTO
    // =============================================

    /**
     * Fecha alerta específico por ID.
     *
     * @param id ID do alerta a fechar
     */
    public void fecharAlerta(String id) {
        Platform.runLater(() -> processarFechamentoAlerta(id));
    }

    /**
     * Fecha todos os alertas ativos.
     */
    public void fecharTodosAlertas() {
        Platform.runLater(() ->
                new ArrayList<>(alertasAtivos.keySet()).forEach(this::processarFechamentoAlerta));
    }

    /**
     * @return Quantidade de alertas ativos
     */
    public int getQuantidadeAlertasAtivos() {
        return alertasAtivos.size();
    }

    /**
     * @return Stage principal
     */
    public javafx.stage.Stage getPrimaryStage() {
        return stageManager != null ? stageManager.getPrimaryStage() : null;
    }

    // =============================================
    // API PÚBLICA - CONFIRMAÇÃO
    // =============================================

    /**
     * Tipo de confirmação.
     */
    public enum TipoConfirmacao {
        PADRAO,  // Sim/Não
        PERIGOSA,  // Aceitar/Desistir (crítica)
        SAIR,  // Salvar/Não Salvar/Sair
    }

    /**
     * Criar confirmação com callback.
     *
     * @param mensagem Mensagem da confirmação
     * @param detalhes Detalhes adicionais (opcional)
     * @param origem Origem (título)
     * @param ownerNode Node proprietário (opcional)
     * @param tipo Tipo de confirmação
     * @param callbackResposta Callback com resposta (true = positivo, false = negativo)
     * @return ID da confirmação
     */
    public String criarConfirmacao(String mensagem, String detalhes,
                                   String origem, Node ownerNode,
                                   TipoConfirmacao tipo, Consumer<Boolean> callbackResposta) {
        String id = UUID.randomUUID().toString();
        Platform.runLater(() ->
                processarCriacaoConfirmacao(id, mensagem, detalhes, origem, ownerNode, tipo, callbackResposta));
        return id;
    }

    /**
     * Confirmação Sim/Não (PADRAO).
     *
     * @param mensagem Mensagem da confirmação
     * @param titulo Título
     * @param callback Callback com resposta
     */
    public void confirmar(String mensagem, String titulo, Consumer<Boolean> callback) {
        criarConfirmacao(mensagem, null, titulo, null, TipoConfirmacao.PADRAO, callback);
    }

    /**
     * Confirmação com detalhes e tipo customizado.
     *
     * @param mensagem Mensagem da confirmação
     * @param detalhes Detalhes adicionais
     * @param titulo Título
     * @param tipo Tipo de confirmação
     * @param callback Callback com resposta
     */
    public void confirmarComDetalhes(String mensagem, String detalhes, String titulo,
                                     TipoConfirmacao tipo, Consumer<Boolean> callback) {
        criarConfirmacao(mensagem, detalhes, titulo, null, tipo, callback);
    }

    // =============================================
    // INTERNO - CRIAÇÃO DE ALERTA
    // =============================================

    /**
     * ModelAlert: modelo de alerta.
     */
    private static class ModelAlert {
        private String titulo;
        private String descricao;
        private String detalhes;
        private AlertType tipo;

        public static ModelAlert builder() {
            return new ModelAlert();
        }

        public ModelAlert titulo(String titulo) {
            this.titulo = titulo;
            return this;
        }

        public ModelAlert descricao(String descricao) {
            this.descricao = descricao;
            return this;
        }

        public ModelAlert detalhes(String detalhes) {
            this.detalhes = detalhes;
            return this;
        }

        public ModelAlert info() {
            this.tipo = AlertType.INFO;
            return this;
        }

        public ModelAlert success() {
            this.tipo = AlertType.SUCCESS;
            return this;
        }

        public ModelAlert warn() {
            this.tipo = AlertType.WARNING;
            return this;
        }

        public ModelAlert erro() {
            this.tipo = AlertType.ERROR;
            return this;
        }

        public ModelAlert critico() {
            this.tipo = AlertType.CRITICAL;
            return this;
        }

        public static ModelAlert info(String titulo, String descricao) {
            return builder().titulo(titulo).descricao(descricao).info();
        }

        public static ModelAlert success(String titulo, String descricao) {
            return builder().titulo(titulo).descricao(descricao).success();
        }

        public static ModelAlert warn(String titulo, String descricao) {
            return builder().titulo(titulo).descricao(descricao).warn();
        }

        public static ModelAlert erro(String titulo, String descricao) {
            return builder().titulo(titulo).descricao(descricao).erro();
        }

        public static ModelAlert critico(String titulo, String descricao) {
            return builder().titulo(titulo).descricao(descricao).critico();
        }

        public String getTitulo() {
            return titulo;
        }

        public String getDescricao() {
            return descricao;
        }

        public String getDetalhes() {
            return detalhes;
        }

        public AlertType getTipo() {
            return tipo;
        }

        public ModelAlert build() {
            return this;
        }
    }

    /**
     * Criar alerta.
     *
     * @param alerta Modelo de alerta
     * @return ID do alerta
     */
    private String criarAlerta(ModelAlert alerta) {
        String id = UUID.randomUUID().toString();

        Platform.runLater(() -> {
            try {
                NotificationType notificationType = switch (alerta.getTipo()) {
                    case INFO -> NotificationType.INFO;
                    case SUCCESS -> NotificationType.SUCCESS;
                    case WARNING -> NotificationType.WARNING;
                    case ERROR, CRITICAL -> NotificationType.ERROR;
                    default -> NotificationType.INFO;
                };

                String viewId = NotificationViewResolver.resolveViewId(notificationType);

                if (!resourceRegistry.contains(viewId)) {
                    return;
                }

                // Abre UNDECORATED com temporizador
                javafx.stage.Stage stage = stageManager.openAlertUndecoratedWithId(viewId, alerta.getTipo(), id);
                alertasAtivos.put(id, stage);

            } catch (Exception e) {
            }
        });

        return id;
    }

    // =============================================
    // INTERNO - FECHAMENTO DE ALERTA
    // =============================================

    /**
     * Processar fechamento de alerta.
     *
     * @param id ID do alerta
     */
    private void processarFechamentoAlerta(String id) {
        javafx.stage.Stage stage = alertasAtivos.remove(id);
        if (stage != null) {
            stage.close();
        }
    }

    // =============================================
    // INTERNO - CRIAÇÃO DE CONFIRMAÇÃO
    // =============================================

    /**
     * Processar criação de confirmação.
     *
     * @param id ID da confirmação
     * @param mensagem Mensagem
     * @param detalhes Detalhes
     * @param origem Origem
     * @param ownerNode Node proprietário
     * @param tipo Tipo de confirmação
     * @param callbackResposta Callback com resposta
     */
    private void processarCriacaoConfirmacao(String id, String mensagem, String detalhes,
                                             String origem, Node ownerNode,
                                             TipoConfirmacao tipo, Consumer<Boolean> callbackResposta) {
        try {
            String viewId = NotificationViewResolver.resolveViewId(
                    tipo == TipoConfirmacao.PERIGOSA ? NotificationType.CRITICAL : NotificationType.CONFIRMATION);

            if (!resourceRegistry.contains(viewId)) {
                // Fallback nativo
                boolean result = showNativeConfirm(mensagem, origem, tipo);
                callbackResposta.accept(result);
                return;
            }

            // Abre diálogo de confirmação
            boolean result = stageManager.openAlertUndecoratedWithResult(viewId,
                    tipo == TipoConfirmacao.PERIGOSA ? AlertType.CRITICAL : AlertType.CONFIRMATION);
            callbackResposta.accept(result);

        } catch (Exception e) {
            boolean result = showNativeConfirm(mensagem, origem, tipo);
            callbackResposta.accept(result);
        }
    }

    // =============================================
    // INTERNO - CONFIRMAÇÃO NATIVA
    // =============================================

    /**
     * Confirmação nativa (fallback).
     *
     * @param mensagem Mensagem
     * @param titulo Título
     * @param tipo Tipo de confirmação
     * @return true se positivo, false se negativo
     */
    private boolean showNativeConfirm(String mensagem, String titulo, TipoConfirmacao tipo) {
        Alert alert = new Alert(tipo == TipoConfirmacao.PERIGOSA ?
                Alert.AlertType.WARNING : Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(titulo);
        alert.setContentText(mensagem);

        Optional<ButtonType> result = alert.showAndWait();
        return result.filter(r -> r == ButtonType.OK).isPresent();
    }
}