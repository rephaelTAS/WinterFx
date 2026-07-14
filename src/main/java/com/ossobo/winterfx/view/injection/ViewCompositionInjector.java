package com.ossobo.winterfx.view.injection;

import com.ossobo.winterfx.di.injection.DependencyInjector;
import com.ossobo.winterfx.scanner.ReflectionScanner;
import com.ossobo.winterfx.view.anotations.InjectView;
import com.ossobo.winterfx.view.callback.ViewLoadedListener;
import com.ossobo.winterfx.view.controller.WinterFXController;
import com.ossobo.winterfx.view.loader.LoadedView;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.LazyViewProxy;
import com.ossobo.winterfx.view.StageManager;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * ViewCompositionInjector v4.0 — Arquitetura MVVM
 *
 * <p>Responsável pela COMPOSIÇÃO de Views (injetar View B dentro da View A).
 * No padrão MVVM do WinterFx, este injetor NÃO lida com o estado reativo dos campos,
 * mas sim com a estrutura visual em árvore das telas.</p>
 *
 * <p>Mantém as features avançadas: Lazy Proxy, Injeção Tardia e Bind de Layout.</p>
 *
 * @version 4.0 (MVVM Adaptation)
 */
public class ViewCompositionInjector implements DependencyInjector, ViewLoadedListener {

    private static final Logger LOGGER = Logger.getLogger(ViewCompositionInjector.class.getName());

    private final ReflectionScanner reflectionScanner;
    private final ResourceRegistry resourceRegistry;
    private final StageManager stageManager;

    // [NOVO] Referência ao estado da View PAI para gerenciamento de ciclo de vida em cascata
    private final ViewState parentViewState;

    private final Map<String, List<InjectionRequest>> pendingInjections = new ConcurrentHashMap<>();

    public ViewCompositionInjector(ReflectionScanner reflectionScanner,
                                   ResourceRegistry resourceRegistry,
                                   StageManager stageManager,
                                   ViewState parentViewState) {
        this.reflectionScanner = reflectionScanner;
        this.resourceRegistry = resourceRegistry;
        this.stageManager = stageManager;
        this.parentViewState = parentViewState;
    }

    // ============================================================
    // VIEW LOADED LISTENER
    // ============================================================

    @Override
    public void onViewLoaded(String viewId) {
        retryPendingInjections(viewId);
    }

    // ============================================================
    // INJEÇÃO PRINCIPAL
    // ============================================================

    @Override
    public void inject(Object instance, Class<?> type) {
        if (resourceRegistry == null || stageManager == null) return;

        List<Field> viewFields = reflectionScanner.getFieldsWithAnnotation(type, InjectView.class);

        for (Field field : viewFields) {
            InjectView annotation = field.getAnnotation(InjectView.class);
            String viewId = annotation.value();

            try {
                Optional<ViewDescriptor> optDescriptor = resourceRegistry.findViewById(viewId);
                if (optDescriptor.isEmpty()) {
                    if (annotation.required()) {
                        throw new IllegalArgumentException("View não registrada para composição: '" + viewId + "'");
                    }
                    continue;
                }

                Class<?> fieldType = field.getType();

                if (Parent.class.isAssignableFrom(fieldType) || Pane.class.isAssignableFrom(fieldType)) {
                    injectViewDirect(instance, field, viewId, annotation);
                } else {
                    injectControllerLazy(instance, field, viewId);
                }

            } catch (Exception e) {
                if (annotation.required()) {
                    throw new RuntimeException("Falha na composição da view: " + viewId, e);
                }
            }
        }
    }

    // ============================================================
    // INJEÇÃO DIRETA (Parent/Pane)
    // ============================================================

    private void injectViewDirect(Object instance, Field field, String viewId, InjectView annotation) {
        if (stageManager.isViewCached(viewId)) {
            LoadedView<?> loadedView = stageManager.loadView(viewId);
            try {
                injectIntoField(instance, field, loadedView.getRoot(), annotation.child());
            } catch (IllegalAccessException e) {
                LOGGER.warning(() -> "Falha ao injetar view na composição: " + viewId);
            }
            return;
        }

        registerPendingViewInjection(instance, field, viewId, annotation.child());
    }

    // ============================================================
    // INJEÇÃO LAZY (Controller Proxy)
    // ============================================================

    @SuppressWarnings("unchecked")
    private void injectControllerLazy(Object instance, Field field, String viewId) {
        try {
            Class<?> fieldType = field.getType();

            if (!WinterFXController.class.isAssignableFrom(fieldType)) {
                injectControllerDirect(instance, field, viewId);
                return;
            }

            // Usa o LazyViewProxy que você já tinha no projeto
            Object proxy = Proxy.newProxyInstance(
                    fieldType.getClassLoader(),
                    new Class<?>[]{WinterFXController.class},
                    new LazyViewProxy<>(stageManager, viewId)
            );

            field.setAccessible(true);
            field.set(instance, proxy);

        } catch (Exception e) {
            throw new RuntimeException("Falha ao criar proxy para composição @InjectView: " + viewId, e);
        }
    }

    private void injectControllerDirect(Object instance, Field field, String viewId) {
        try {
            LoadedView<?> loaded = stageManager.loadView(viewId);
            Object controller = loaded.getController();
            if (controller != null) {
                field.setAccessible(true);
                field.set(instance, controller);
            }
        } catch (Exception ignored) {}
    }

    // ============================================================
    // INJEÇÃO TARDIA
    // ============================================================

    private void registerPendingViewInjection(Object instance, Field field, String viewId, String childId) {
        pendingInjections.computeIfAbsent(viewId, k -> new CopyOnWriteArrayList<>())
                .add(new InjectionRequest(instance, field, childId));
    }

    private void retryPendingInjections(String viewId) {
        List<InjectionRequest> requests = pendingInjections.remove(viewId);
        if (requests == null || requests.isEmpty()) return;

        LoadedView<?> loaded = stageManager.loadView(viewId);
        for (InjectionRequest req : requests) {
            try {
                injectIntoField(req.instance, req.field, loaded.getRoot(), req.childId);
            } catch (Exception ignored) {}
        }
    }

    // ============================================================
    // INJEÇÃO NO CAMPO (Lógica de Layout original preservada)
    // ============================================================

    private void injectIntoField(Object instance, Field field, Parent view, String childId)
            throws IllegalAccessException {

        Class<?> fieldType = field.getType();
        field.setAccessible(true);

        Parent targetView = view;
        if (childId != null && !childId.isEmpty()) {
            Node child = view.lookup("#" + childId);
            if (child instanceof Parent p) targetView = p;
        }

        if (Pane.class.isAssignableFrom(fieldType)) {
            Pane pane = (Pane) field.get(instance);
            if (pane != null) {
                pane.getChildren().clear();
                pane.getChildren().add(targetView);

                if (pane instanceof AnchorPane) {
                    AnchorPane.setTopAnchor(targetView, 0.0);
                    AnchorPane.setBottomAnchor(targetView, 0.0);
                    AnchorPane.setLeftAnchor(targetView, 0.0);
                    AnchorPane.setRightAnchor(targetView, 0.0);
                } else if (pane instanceof BorderPane bp) {
                    bp.setCenter(targetView);
                }

                if (targetView instanceof Region region) {
                    region.prefWidthProperty().bind(pane.widthProperty());
                    region.prefHeightProperty().bind(pane.heightProperty());
                }
            }
        } else if (Parent.class.isAssignableFrom(fieldType)) {
            field.set(instance, targetView);
        }
    }

    // ============================================================
    // INNER CLASS
    // ============================================================

    private static class InjectionRequest {
        final Object instance;
        final Field field;
        final String childId;

        InjectionRequest(Object instance, Field field, String childId) {
            this.instance = instance;
            this.field = field;
            this.childId = childId;
        }
    }
}