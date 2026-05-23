package com.ossobo.winterfx.AlertSystem.model;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class AlertInfo {
    public final String id;
    public final Stage stage;
    public final TipoAlerta tipo;
    public final Modalidade modalidade;
    final Node ownerNode;
    public final Pane overlayBlock;

    public AlertInfo(String id, Stage stage, TipoAlerta tipo,
                     Modalidade modalidade, Node ownerNode, Pane overlayBlock) {
        this.id = id;
        this.stage = stage;
        this.tipo = tipo;
        this.modalidade = modalidade;
        this.ownerNode = ownerNode;
        this.overlayBlock = overlayBlock;
    }
}

