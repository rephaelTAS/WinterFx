package com.ossobo.winterfx.AlertSystem;

import com.ossobo.winterfx.AlertSystem.core.AlertaSystem;
import com.ossobo.winterfx.AlertSystem.model.Modalidade;
import com.ossobo.winterfx.AlertSystem.model.ModelAlert;
import com.ossobo.winterfx.AlertSystem.model.TipoAlerta;
import com.ossobo.winterfx.AlertSystem.model.TipoConfirmacao;
import com.ossobo.winterfx.di.annotations.Component;
import com.ossobo.winterfx.di.annotations.ScopeAnnotation;


import com.ossobo.winterfx.di.scopes.enums.ScopeType;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

/**
 * 🎯 Sistema de Alertas Inteligente - NexusFX
 *
 * v2.0 (23/04/2026):
 * - ✅ Suporte a ModelAlert
 * - ✅ API simplificada mantida
 * - ✅ Compatibilidade total
 */
@Component
@ScopeAnnotation(ScopeType.SINGLETON)
public class SystemsAlerty {
    private final Stage primaryStage;
    private final AlertaSystem sistema = AlertaSystem.getInstance();

    public SystemsAlerty(Stage primaryStage) {
        this.primaryStage = primaryStage;
        setPrimaryStage();
    }

    public void setPrimaryStage() {
        sistema.setPrimaryStage(this.primaryStage);
    }

    // =============================================
    // 🆕 MÉTODO PRINCIPAL VIA ModelAlert
    // =============================================

    /**
     * ✅ Cria alerta a partir de ModelAlert
     */
    public String criarAlerta(ModelAlert modelAlert) {
        return sistema.criarAlerta(
                modelAlert.getTitulo(),
                modelAlert.getDescricao(),
                modelAlert.getDetalhes(),
                modelAlert.getOrigem(),
                modelAlert.getOwnerNode(),
                modelAlert.getTipo(),
                modelAlert.getModalidade()
        );
    }

    // =============================================
    // 🔵 MÉTODOS INFO
    // =============================================

    public String info(String titulo, String descricao, String origem) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).origem(origem).info().build());
    }

    public String info(String titulo, String descricao, String detalhes, String origem) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes).origem(origem).info().build());
    }

    public String info(String titulo, String descricao, String detalhes,
                       String origem, javafx.scene.Node ownerNode) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes)
                .origem(origem).ownerNode(ownerNode).info().build());
    }

    // =============================================
    // 🟡 MÉTODOS WARN
    // =============================================

    public String warn(String titulo, String descricao, String origem) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).origem(origem).warn().build());
    }

    public String warn(String titulo, String descricao, String detalhes, String origem) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes).origem(origem).warn().build());
    }

    public String warn(String titulo, String descricao, String detalhes,
                       String origem, javafx.scene.Node ownerNode) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes)
                .origem(origem).ownerNode(ownerNode).warn().build());
    }

    // =============================================
    // 🔴 MÉTODOS ERRO
    // =============================================

    public String erro(String titulo, String descricao, String origem) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).origem(origem).erro().build());
    }

    public String erro(String titulo, String descricao, String detalhes, String origem) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes).origem(origem).erro().build());
    }

    public String erro(String titulo, String descricao, String detalhes,
                       String origem, javafx.scene.Node ownerNode) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes)
                .origem(origem).ownerNode(ownerNode).erro().build());
    }

    // =============================================
    // ⚫ MÉTODOS CRITICAL
    // =============================================

    public String critical(String titulo, String descricao, String origem) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).origem(origem).critico().build());
    }

    public String critical(String titulo, String descricao, String detalhes, String origem) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes).origem(origem).critico().build());
    }

    public String critical(String titulo, String descricao, String detalhes,
                           String origem, javafx.scene.Node ownerNode) {
        return criarAlerta(ModelAlert.builder()
                .titulo(titulo).descricao(descricao).detalhes(detalhes)
                .origem(origem).ownerNode(ownerNode).critico().build());
    }

    // =============================================
    // ❓ MÉTODOS DE CONFIRMAÇÃO
    // =============================================

    public void confirmar(String mensagem, String titulo, Consumer<Boolean> callback) {
        sistema.criarConfirmacao(mensagem, null, titulo, null,
                TipoConfirmacao.PADRAO, callback);
    }

    public void confirmarComDetalhes(String mensagem, String detalhes, String titulo,
                                     TipoConfirmacao tipo, Consumer<Boolean> callback) {
        sistema.criarConfirmacao(mensagem, detalhes, titulo, null, tipo, callback);
    }

    public void confirmar(String mensagem, String titulo,
                          javafx.scene.Node ownerNode, Consumer<Boolean> callback) {
        sistema.criarConfirmacao(mensagem, null, titulo, ownerNode,
                TipoConfirmacao.PADRAO, callback);
    }

    public void confirmar(String mensagem, String titulo,
                          TipoConfirmacao tipo, Consumer<Boolean> callback) {
        sistema.criarConfirmacao(mensagem, null, titulo, null, tipo, callback);
    }

    public void confirmar(String mensagem, String detalhes, String titulo,
                          javafx.scene.Node ownerNode,
                          TipoConfirmacao tipo, Consumer<Boolean> callback) {
        sistema.criarConfirmacao(mensagem, detalhes, titulo, ownerNode, tipo, callback);
    }

    public void confirmarPerigo(String mensagem, String detalhes, String titulo,
                                Consumer<Boolean> callback) {
        sistema.criarConfirmacao(mensagem, detalhes, titulo, null,
                TipoConfirmacao.PERIGO, callback);
    }

    public void confirmarAviso(String mensagem, String detalhes, String titulo,
                               Consumer<Boolean> callback) {
        sistema.criarConfirmacao(mensagem, detalhes, titulo, null,
                TipoConfirmacao.AVISO, callback);
    }

    public void confirmarInfo(String mensagem, String detalhes, String titulo,
                              Consumer<Boolean> callback) {
        sistema.criarConfirmacao(mensagem, detalhes, titulo, null,
                TipoConfirmacao.INFO, callback);
    }

    public void confirmarSucesso(String mensagem, String detalhes, String titulo,
                                 Consumer<Boolean> callback) {
        sistema.criarConfirmacao(mensagem, detalhes, titulo, null,
                TipoConfirmacao.SUCESSO, callback);
    }

    // =============================================
    // 🔄 MÉTODOS UTILITÁRIOS
    // =============================================

    public void fecharAlerta(String id) {
        sistema.fecharAlerta(id);
    }

    public void fecharTodosAlertas() {
        sistema.fecharTodosAlertas();
    }

    public int getQuantidadeAlertasAtivos() {
        return sistema.getQuantidadeAlertasAtivos();
    }

    public boolean isAlertaAtivo(String id) {
        return sistema.isAlertaAtivo(id);
    }

    public String getInfoAlerta(String id) {
        return sistema.isAlertaAtivo(id) ? "Alerta ativo: " + id : "Alerta não encontrado: " + id;
    }

    public List<String> listarAlertasAtivos() {
        return sistema.listarAlertasAtivos();
    }

    public void setVolume(double volume) {
        sistema.setVolume(volume);
    }

    public void diagnosticarSistema() {
        sistema.diagnosticarSistema();
    }

    public void limparTodosOverlays() {
        sistema.limparTodosOverlays();
    }

    // =============================================
    // 🎭 MÉTODOS DE BAIXO NÍVEL
    // =============================================

    public String criarAlertaCustomizado(String titulo, String descricao, String detalhes,
                                         String origem, javafx.scene.Node ownerNode,
                                         TipoAlerta tipo, Modalidade modalidade) {
        return sistema.criarAlerta(titulo, descricao, detalhes, origem, ownerNode, tipo, modalidade);
    }

    public String criarConfirmacaoCustomizada(String mensagem, String detalhes, String origem,
                                              javafx.scene.Node ownerNode,
                                              TipoConfirmacao tipo, Consumer<Boolean> callback) {
        return sistema.criarConfirmacao(mensagem, detalhes, origem, ownerNode, tipo, callback);
    }
}
