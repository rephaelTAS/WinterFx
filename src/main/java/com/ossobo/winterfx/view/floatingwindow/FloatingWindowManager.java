package com.ossobo.winterfx.view.floatingwindow;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.view.floatingwindow.anotations.FloatingWindow;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.view.floatingwindow.enums.Modality;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.loader.LoadedView;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🪟 FloatingWindowManager v5.2 - Com loadFloatingView + fresh
 */
public class FloatingWindowManager {

    private final ResourceRegistry registry;
    private final StageManager stageManager;
    private final DiContainer diContainer;

    private final Map<String, Stage> managedWindows = new ConcurrentHashMap<>();
    private final Deque<Stage> modalStack = new ArrayDeque<>();
    private int instanceCounter = 0;

    public FloatingWindowManager(ResourceRegistry registry, StageManager stageManager, DiContainer diContainer) {
        this.registry = registry;
        this.stageManager = stageManager;
        this.diContainer = diContainer;
    }

    public void processAnnotations(Object bean) {
        if (bean == null) return;
        Class<?> clazz = bean.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            FloatingWindow ann = field.getAnnotation(FloatingWindow.class);
            if (ann != null) processFloatingWindow(bean, field, ann);
        }
    }

    private void processFloatingWindow(Object bean, Field field, FloatingWindow annotation) {
        String viewId = annotation.viewId();
        try {
            ViewDescriptor descriptor = registry.findViewById(viewId)
                    .orElseThrow(() -> new IllegalArgumentException("View não registrada: '" + viewId + "'"));

            // Singleton: traz para frente
            if (annotation.singleton() && !annotation.multipleInstances()) {
                Stage existing = managedWindows.get(viewId);
                if (existing != null && existing.isShowing()) {
                    existing.toFront();
                    field.setAccessible(true);
                    field.set(bean, existing);
                    return;
                }
            }

            // 🔥 loadFloatingView: fresh=true → nova instância, fresh=false → cache
            LoadedView<?> loadedView = stageManager.loadFloatingView(viewId, annotation.fresh());

            Stage stage = new Stage();
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.setTitle(!annotation.title().isEmpty() ? annotation.title() : descriptor.getTitle());

            javafx.stage.Modality modality = convertModality(annotation.modality());
            stage.initModality(modality);

            Window owner = getTopModalOrPrimary();
            if (owner != null && owner != stage) stage.initOwner(owner);

            Scene scene = new Scene(loadedView.getRoot(),
                    annotation.width() > 0 ? annotation.width() : descriptor.getWidth(),
                    annotation.height() > 0 ? annotation.height() : descriptor.getHeight());
            stage.setScene(scene);
            stage.setResizable(annotation.resizable());
            stage.setAlwaysOnTop(annotation.alwaysOnTop());
            if (descriptor.isCentered()) stage.centerOnScreen();

            String stageKey = annotation.multipleInstances() ? viewId + "-" + (++instanceCounter) : viewId;
            managedWindows.put(stageKey, stage);

            stage.setOnShown(e -> {
                if (modality != javafx.stage.Modality.NONE) modalStack.push(stage);
            });
            stage.setOnHidden(e -> {
                modalStack.remove(stage);
                if (!annotation.multipleInstances()) managedWindows.remove(stageKey);
            });

            field.setAccessible(true);
            field.set(bean, stage);

            if (annotation.autoOpen()) stage.show();

        } catch (Exception e) {
        }
    }

    private Window getTopModalOrPrimary() {
        if (!modalStack.isEmpty()) {
            Stage top = modalStack.peek();
            if (top.isShowing()) return top;
        }
        return Stage.getWindows().stream()
                .filter(w -> w instanceof Stage && w.isShowing())
                .findFirst().orElse(null);
    }

    public void abrir(String viewId) {
        Stage stage = managedWindows.get(viewId);
        if (stage != null) { if (stage.isShowing()) stage.toFront(); else stage.show(); }
    }
    public void fechar(String viewId) { Stage s = managedWindows.remove(viewId); if (s != null) s.close(); }
    public void fecharTodas() { managedWindows.values().forEach(Stage::close); managedWindows.clear(); modalStack.clear(); }

    private javafx.stage.Modality convertModality(Modality m) {
        return switch (m) {
            case APPLICATION_MODAL -> javafx.stage.Modality.APPLICATION_MODAL;
            case WINDOW_MODAL -> javafx.stage.Modality.WINDOW_MODAL;
            case NONE -> javafx.stage.Modality.NONE;
            default -> javafx.stage.Modality.NONE;  // 🆕 Fallback seguro
        };
    }
}