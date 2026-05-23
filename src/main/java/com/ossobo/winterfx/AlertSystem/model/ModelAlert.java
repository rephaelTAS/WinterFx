package com.ossobo.winterfx.AlertSystem.model;



import javafx.scene.Node;

/**
 * 🎯 MODEL ALERT - Modelo de dados para criação de alertas
 *
 * v1.0 (23/04/2026):
 * - ✅ Encapsula todos os parâmetros de um alerta
 * - ✅ Builder fluente para criação simplificada
 * - ✅ Imutável após construção
 *
 * <pre>
 * Uso:
 *   ModelAlert alert = ModelAlert.builder()
 *       .titulo("Erro")
 *       .descricao("Falha ao salvar")
 *       .tipo(TipoAlerta.ERRO)
 *       .modalidade(Modalidade.MODAL)
 *       .build();
 *
 *   alertaSystem.criarAlerta(alert);
 * </pre>
 */
public final class ModelAlert {

    private final String titulo;
    private final String descricao;
    private final String detalhes;
    private final String origem;
    private final Node ownerNode;
    private final TipoAlerta tipo;
    private final Modalidade modalidade;

    /**
     * Construtor privado - use o Builder
     */
    private ModelAlert(Builder builder) {
        this.titulo = builder.titulo;
        this.descricao = builder.descricao;
        this.detalhes = builder.detalhes;
        this.origem = builder.origem;
        this.ownerNode = builder.ownerNode;
        this.tipo = builder.tipo != null ? builder.tipo : TipoAlerta.INFO;
        this.modalidade = builder.modalidade != null ? builder.modalidade : Modalidade.SEMI_MODAL;
    }

    // ==================== GETTERS ====================

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public String getOrigem() {
        return origem;
    }

    public Node getOwnerNode() {
        return ownerNode;
    }

    public TipoAlerta getTipo() {
        return tipo;
    }

    public Modalidade getModalidade() {
        return modalidade;
    }

    /**
     * Verifica se tem detalhes
     */
    public boolean hasDetalhes() {
        return detalhes != null && !detalhes.isEmpty();
    }

    /**
     * Verifica se tem owner node
     */
    public boolean hasOwnerNode() {
        return ownerNode != null;
    }

    // ==================== BUILDER ====================

    /**
     * Cria um novo Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder para construção fluente
     */
    public static class Builder {
        private String titulo;
        private String descricao;
        private String detalhes;
        private String origem;
        private Node ownerNode;
        private TipoAlerta tipo = TipoAlerta.INFO;
        private Modalidade modalidade = Modalidade.SEMI_MODAL;

        public Builder titulo(String titulo) {
            this.titulo = titulo;
            return this;
        }

        public Builder descricao(String descricao) {
            this.descricao = descricao;
            return this;
        }

        public Builder detalhes(String detalhes) {
            this.detalhes = detalhes;
            return this;
        }

        public Builder origem(String origem) {
            this.origem = origem;
            return this;
        }

        public Builder ownerNode(Node ownerNode) {
            this.ownerNode = ownerNode;
            return this;
        }

        public Builder tipo(TipoAlerta tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder modalidade(Modalidade modalidade) {
            this.modalidade = modalidade;
            return this;
        }

        // ===== MÉTODOS DE CONVENIÊNCIA =====

        /**
         * Configura como alerta de informação
         */
        public Builder info() {
            this.tipo = TipoAlerta.INFO;
            this.modalidade = Modalidade.NAO_MODAL;
            return this;
        }

        /**
         * Configura como alerta de aviso
         */
        public Builder warn() {
            this.tipo = TipoAlerta.WARN;
            this.modalidade = Modalidade.SEMI_MODAL;
            return this;
        }

        /**
         * Configura como alerta de erro
         */
        public Builder erro() {
            this.tipo = TipoAlerta.ERRO;
            this.modalidade = Modalidade.MODAL;
            return this;
        }

        /**
         * Configura como alerta crítico
         */
        public Builder critico() {
            this.tipo = TipoAlerta.CRITICAL;
            this.modalidade = Modalidade.MODAL;
            return this;
        }

        /**
         * Constrói o ModelAlert
         */
        public ModelAlert build() {
            if (titulo == null || titulo.trim().isEmpty()) {
                titulo = tipo != null ? tipo.name() : "Alerta";
            }
            return new ModelAlert(this);
        }
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Cria alerta de informação rápido
     */
    public static ModelAlert info(String titulo, String descricao) {
        return builder()
                .titulo(titulo)
                .descricao(descricao)
                .info()
                .build();
    }

    /**
     * Cria alerta de aviso rápido
     */
    public static ModelAlert warn(String titulo, String descricao) {
        return builder()
                .titulo(titulo)
                .descricao(descricao)
                .warn()
                .build();
    }

    /**
     * Cria alerta de erro rápido
     */
    public static ModelAlert erro(String titulo, String descricao) {
        return builder()
                .titulo(titulo)
                .descricao(descricao)
                .erro()
                .build();
    }

    /**
     * Cria alerta crítico rápido
     */
    public static ModelAlert critico(String titulo, String descricao) {
        return builder()
                .titulo(titulo)
                .descricao(descricao)
                .critico()
                .build();
    }

    // ==================== TO STRING ====================

    @Override
    public String toString() {
        return String.format("ModelAlert[tipo=%s, modalidade=%s, titulo='%s']",
                tipo, modalidade, titulo);
    }
}
