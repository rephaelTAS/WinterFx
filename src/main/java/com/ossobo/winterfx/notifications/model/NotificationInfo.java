// NotificationInfo.java v2.0 - 2026-08-22
// Migrado de classe imutável manual para Record Java 17+
package com.ossobo.winterfx.notifications.model;

import com.ossobo.winterfx.notifications.enums.NotificationType;
import com.ossobo.winterfx.resources.enums.Modality;

import java.util.Objects;

/**
 * 📋 NotificationInfo v2.0
 *
 * Representa os dados imutáveis de uma notificação/alerta (Record Java 17+).
 *
 * <p>Campos:
 * <ul>
 *   <li><b>titulo</b> - Título da notificação (obrigatório)</li>
 *   <li><b>descricao</b> - Mensagem principal (obrigatório)</li>
 *   <li><b>detalhes</b> - Detalhes técnicos (opcional, para erros)</li>
 *   <li><b>origem</b> - Origem da notificação (classe/método)</li>
 *   <li><b>tipo</b> - Tipo da notificação (SUCCESS, ERROR, WARNING, INFO)</li>
 *   <li><b>modalidade</b> - Se bloqueia ou não (MODAL, NAO_MODAL)</li>
 * </ul>
 *
 * @version 2.0 (22/08/2026) - Transformado em Record
 */
public record NotificationInfo(
        String titulo,
        String descricao,
        String detalhes,
        String origem,
        NotificationType tipo,
        Modality modalidade
) {

    /**
     * Construtor compacto para validação e limpeza de nulos.
     */
    public NotificationInfo {
        Objects.requireNonNull(titulo, "titulo é obrigatório");
        Objects.requireNonNull(descricao, "descricao é obrigatório");
        if (titulo.isEmpty()) throw new IllegalArgumentException("titulo não pode ser vazio");
        if (descricao.isEmpty()) throw new IllegalArgumentException("descricao não pode ser vazio");

        detalhes = (detalhes != null && !detalhes.isBlank()) ? detalhes : null;
        tipo = Objects.requireNonNullElse(tipo, NotificationType.INFO);
        modalidade = Objects.requireNonNullElse(modalidade, Modality.NAO_MODAL);
    }

    // ===== Métodos de Negócio =====

    public boolean hasDetalhes() {
        return detalhes != null && !detalhes.isEmpty();
    }

    public boolean isModal() {
        return modalidade == Modality.MODAL;
    }

    // ===== Builder =====

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String titulo;
        private String descricao;
        private String detalhes;
        private String origem;
        private NotificationType tipo = NotificationType.INFO;
        private Modality modalidade = Modality.NAO_MODAL;

        public Builder titulo(String titulo)             { this.titulo = titulo; return this; }
        public Builder descricao(String descricao)       { this.descricao = descricao; return this; }
        public Builder detalhes(String detalhes)         { this.detalhes = detalhes; return this; }
        public Builder origem(String origem)             { this.origem = origem; return this; }
        public Builder tipo(NotificationType tipo)       { this.tipo = tipo; return this; }
        public Builder modalidade(Modality modalidade) { this.modalidade = modalidade; return this; }

        /** Atalho: SUCCESS (NAO_MODAL) */
        public Builder success(String titulo, String descricao) {
            this.titulo = titulo;
            this.descricao = descricao;
            this.tipo = NotificationType.SUCCESS;
            this.modalidade = Modality.NAO_MODAL;
            return this;
        }

        /** Atalho: ERROR (MODAL) */
        public Builder error(String titulo, String descricao) {
            this.titulo = titulo;
            this.descricao = descricao;
            this.tipo = NotificationType.ERROR;
            this.modalidade = Modality.MODAL;
            return this;
        }

        /** Atalho: ERROR com detalhes técnicos */
        public Builder error(String titulo, String descricao, String detalhes, String origem) {
            this.titulo = titulo;
            this.descricao = descricao;
            this.detalhes = detalhes;
            this.origem = origem;
            this.tipo = NotificationType.ERROR;
            this.modalidade = Modality.MODAL;
            return this;
        }

        /** Atalho: WARNING (NAO_MODAL) */
        public Builder warning(String titulo, String descricao) {
            this.titulo = titulo;
            this.descricao = descricao;
            this.tipo = NotificationType.WARNING;
            this.modalidade = Modality.NAO_MODAL;
            return this;
        }

        /** Atalho: INFO (NAO_MODAL) */
        public Builder info(String titulo, String descricao) {
            this.titulo = titulo;
            this.descricao = descricao;
            this.tipo = NotificationType.INFO;
            this.modalidade = Modality.NAO_MODAL;
            return this;
        }

        public NotificationInfo build() {
            // Validações são delegadas ao construtor compacto do Record
            return new NotificationInfo(titulo, descricao, detalhes, origem, tipo, modalidade);
        }
    }

    @Override
    public String toString() {
        return String.format("NotificationInfo{tipo=%s, titulo='%s', descricao='%s', detalhes='%s', origem='%s', modalidade=%s}",
                tipo, titulo, descricao, detalhes, origem, modalidade);
    }
}