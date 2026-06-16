package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.di.reflection.ReflectionCache;
import com.ossobo.winterfx.di.reflection.ReflectionProcessor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.anotations.InjectView;
import com.ossobo.winterfx.view.loader.FXMLService;
import com.ossobo.winterfx.view.loader.LoadedView;

import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * ViewInjector v1.3
 *
 * Injetor de views FXML via @InjectView.
 * Carrega o FXML com controller totalmente funcional (injeção + handlers + initialize).
 * Suporta inicialização tardia e preenchimento automático do container.
 */
public class ViewInjector implements DependencyInjector {

    private final ReflectionCache reflectionCache;
    private final ReflectionProcessor reflectionProcessor;
    private final ResourceRegistry resourceRegistry;
    private final FXMLService fxmlService;

    public ViewInjector(ReflectionCache reflectionCache,
                        ReflectionProcessor reflectionProcessor,
                        ResourceRegistry resourceRegistry,
                        FXMLService fxmlService) {
        this.reflectionCache = reflectionCache;
        this.reflectionProcessor = reflectionProcessor;
        this.resourceRegistry = resourceRegistry;
        this.fxmlService = fxmlService;
    }

    @Override
    public void inject(Object instance, Class<?> type) {
        if (resourceRegistry == null || fxmlService == null) {
            return;
        }

        List<Field> viewFields = reflectionCache.getInjectViewFields(type);

        for (Field field : viewFields) {
            InjectView annotation = field.getAnnotation(InjectView.class);
            String viewId = annotation.value();

            try {
                Optional<ViewDescriptor> optDescriptor = resourceRegistry.findViewById(viewId);

                if (optDescriptor.isEmpty()) {
                    if (annotation.required()) {
                        throw new IllegalArgumentException("View não registrada: '" + viewId + "'");
                    }
                    continue;
                }

                ViewDescriptor descriptor = optDescriptor.get();

                // Carrega o FXML com controller totalmente funcional
                LoadedView<?> loadedView = fxmlService.load(descriptor, Object.class);
                Parent view = loadedView.getRoot();
                Object viewController = loadedView.getController();

                // Força initialize() se existir (para @FXML como carregarDados)
                if (viewController != null) {
                    try {
                        Method initMethod = viewController.getClass().getMethod("initialize");
                        initMethod.setAccessible(true);
                        initMethod.invoke(viewController);
                    } catch (NoSuchMethodException ignored) {
                        // Não tem initialize()
                    }
                }

                // Injeta no campo com preenchimento automático
                injectIntoField(instance, field, view, annotation.child());

            } catch (Exception e) {
                if (annotation.required()) {
                    throw new RuntimeException("Falha ao injetar view: " + viewId, e);
                }
            }
        }
    }

    /**
     * Injeta o Parent no campo, com preenchimento automático para todos os Pane.
     *
     * Comportamento por tipo de container:
     * - AnchorPane: âncoras TOP/BOTTOM/LEFT/RIGHT = 0
     * - BorderPane: setCenter(view)
     * - ScrollPane: setContent(view)
     * - StackPane, VBox, HBox, GridPane, FlowPane, Pane: bind width/height
     * - Parent/Node: injeção direta
     */
    private void injectIntoField(Object instance, Field field, Parent view, String childId)
            throws IllegalAccessException {

        Class<?> fieldType = field.getType();
        field.setAccessible(true);

        if (Pane.class.isAssignableFrom(fieldType)) {
            Pane pane = (Pane) field.get(instance);
            if (pane != null) {
                pane.getChildren().clear();
                pane.getChildren().add(view);

                if (pane instanceof AnchorPane) {
                    AnchorPane.setTopAnchor(view, 0.0);
                    AnchorPane.setBottomAnchor(view, 0.0);
                    AnchorPane.setLeftAnchor(view, 0.0);
                    AnchorPane.setRightAnchor(view, 0.0);
                } else if (pane instanceof BorderPane bp) {
                    bp.setCenter(view);
                }

                if (view instanceof Region region) {
                    region.prefWidthProperty().bind(pane.widthProperty());
                    region.prefHeightProperty().bind(pane.heightProperty());
                }
            }
        } else {
            reflectionProcessor.injectField(instance, field, view);
        }
    }
}