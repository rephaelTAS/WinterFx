package com.ossobo.winterfx.notifications.model;


import com.ossobo.winterfx.notifications.enums.NotificationType;
import com.ossobo.winterfx.view.floatingwindow.enums.Modality;

/**
 * 📋 NotificationInfo v1.0
 *
 * Representa os dados de uma notificação/alerta.
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
 */
public final class NotificationInfo {



    private final String titulo;
    private final String descricao;
    private final String detalhes;
    private final String origem;
    private final NotificationType tipo;
    private final Modality modalidade;

    private NotificationInfo(Builder builder) {
        this.titulo = builder.titulo;
        this.descricao = builder.descricao;
        this.detalhes = builder.detalhes;
        this.origem = builder.origem;
        this.tipo = builder.tipo;
        this.modalidade = builder.modalidade;
    }

    public String getTitulo()        { return titulo; }
    public String getDescricao()     { return descricao; }
    public String getDetalhes()      { return detalhes; }
    public String getOrigem()        { return origem; }
    public NotificationType getTipo() { return tipo; }
    public Modality getModalidade() { return modalidade; }

    public boolean hasDetalhes()     { return detalhes != null && !detalhes.isEmpty(); }
    public boolean isModal()         { return modalidade == Modality.MODAL; }

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
            if (titulo == null || titulo.isEmpty()) {
                throw new IllegalArgumentException("titulo é obrigatório");
            }
            if (descricao == null || descricao.isEmpty()) {
                throw new IllegalArgumentException("descricao é obrigatório");
            }
            return new NotificationInfo(this);
        }
    }

    @Override
    public String toString() {
        return String.format("NotificationInfo{tipo=%s, titulo='%s', descricao='%s', detalhes='%s', origem='%s', modalidade=%s}",
                tipo, titulo, descricao, detalhes, origem, modalidade);
    }
}