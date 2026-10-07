package com.ossobo.winterfx.view.floatingwindow;

import com.ossobo.winterfx.resources.enums.Modality;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.loader.LoadedView;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Objects;

/**
 * Wrapper LAZY para janelas flutuantes.
 *
 * <p>A view SÓ é carregada quando {@link #show()} é chamado.</p>
 *
 * @version 1.0 (24/08/2026)
 */
public class StageForFloatingWindow {

    private final StageManager stageManager;
    private final String viewId;
    private final boolean singleton;
    private final String title;
    private final double width;
    private final double height;
    private final boolean resizable;
    private final boolean alwaysOnTop;
    private final boolean centered;
    private final boolean autoClose;
    private final boolean autoOpen;
    private final Modality modality;
    private final String ownerId;
    private final FloatingWindowManager manager;

    private Stage stage;
    private boolean loaded = false;

    public StageForFloatingWindow(StageManager stageManager,
                                  String viewId,
                                  boolean singleton,
                                  String title,
                                  double width,
                                  double height,
                                  boolean resizable,
                                  boolean alwaysOnTop,
                                  boolean centered,
                                  boolean autoClose,
                                  boolean autoOpen,
                                  Modality modality,
                                  String ownerId,
                                  FloatingWindowManager manager) {
        this.stageManager = Objects.requireNonNull(stageManager);
        this.viewId = Objects.requireNonNull(viewId);
        this.singleton = singleton;
        this.title = title;
        this.width = width;
        this.height = height;
        this.resizable = resizable;
        this.alwaysOnTop = alwaysOnTop;
        this.centered = centered;
        this.autoClose = autoClose;
        this.autoOpen = autoOpen;
        this.modality = modality;
        this.ownerId = ownerId;
        this.manager = Objects.requireNonNull(manager);
    }

    /**
     * Exibe a janela flutuante.
     * A view SÓ é carregada neste momento (LAZY).
     */
    public void show() {
        getOrCreateStage().show();
    }

    /**
     * Fecha a janela flutuante.
     */
    public void close() {
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Obtém ou cria o Stage (LAZY).
     */
    private Stage getOrCreateStage() {
        if (stage != null && stage.isShowing()) {
            stage.toFront();
            return stage;
        }

        // Carrega a view
        LoadedView<?> loadedView = stageManager.loadFloatingView(viewId, singleton);

        // Busca o descriptor para herdar tamanho/estilo quando a anotação não define
        var descriptor = stageManager.getDescriptor(viewId);

        stage = new Stage();

        // Estilo: anotação > descriptor > fallback
        stage.initStyle(javafx.stage.StageStyle.UNDECORATED);

        String stageTitle = (title != null && !title.isEmpty())
                ? title
                : (descriptor.title() != null && !descriptor.title().isEmpty())
                ? descriptor.title()
                : viewId;
        stage.setTitle(stageTitle);

        javafx.stage.Modality javaFxModality = manager.convertModality(modality);
        stage.initModality(javaFxModality);

        Window owner = manager.resolveOwner(ownerId);
        if (owner != null && owner != stage) {
            stage.initOwner(owner);
        }

        // ✅ TAMANHO: anotação (>0) > descriptor (>0) > FXML (sizeToScene)
        double w = width > 0  ? width  : descriptor.width();
        double h = height > 0 ? height : descriptor.height();

        Scene scene;
        if (w > 0 && h > 0) {
            scene = new Scene(loadedView.root(), w, h);
        } else {
            scene = new Scene(loadedView.root());
        }
        stage.setScene(scene);

        // ✅ Resizable/alwaysOnTop/centered: anotação vence, descriptor como fallback
        stage.setResizable(resizable);
        stage.setAlwaysOnTop(alwaysOnTop || descriptor.alwaysOnTop());

        // ✅ Aplica min/max do descriptor (se existirem)
        if (descriptor.minWidth()  > 0) stage.setMinWidth(descriptor.minWidth());
        if (descriptor.minHeight() > 0) stage.setMinHeight(descriptor.minHeight());
        if (descriptor.maxWidth()  > 0) stage.setMaxWidth(descriptor.maxWidth());
        if (descriptor.maxHeight() > 0) stage.setMaxHeight(descriptor.maxHeight());

        // ✅ Se ninguém definiu tamanho, deixa o FXML mandar
        if (w <= 0 || h <= 0) {
            stage.sizeToScene();
        }

        if (centered || descriptor.centered()) {
            stage.centerOnScreen();
        }

        // autoClose, modal stack, registerWindow, autoOpen — igual antes
        if (autoClose) {
            stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && stage.isShowing()) stage.close();
            });
        }

        if (javaFxModality != javafx.stage.Modality.NONE) {
            stage.setOnShown(e -> manager.pushModalStack(stage));
        }

        stage.setOnHidden(e -> {
            manager.popModalStack(stage);
            if (singleton) manager.unregisterWindow(viewId);
        });

        String key = singleton ? viewId : manager.generateKey(viewId);
        manager.registerWindow(key, stage);

        loaded = true;

        if (autoOpen) stage.show();

        return stage;
    }

    /**
     * Verifica se a janela está visível.
     */
    public boolean isShowing() {
        return stage != null && stage.isShowing();
    }

    /**
     * Obtém o Stage subjacente (pode ser null se ainda não foi carregado).
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Verifica se a view já foi carregada.
     */
    public boolean isLoaded() {
        return loaded;
    }
}