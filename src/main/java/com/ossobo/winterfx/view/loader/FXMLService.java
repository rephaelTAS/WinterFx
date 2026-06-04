package com.ossobo.winterfx.view.loader;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.runtime.AnnotationRuntime;
import com.ossobo.winterfx.view.exceptios.ViewEngineException;
import com.ossobo.winterfx.view.refresh.RefreshableController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.function.Consumer;

/**
 * FXMLService v5.0
 *
 * Carrega FXML e gerencia handlers de botões.
 * Delega o processamento de anotações para o {@link AnnotationRuntime}.
 *
 * <p>Fluxo:</p>
 * <ol>
 *   <li>Obtém controller do {@code DiContainer}</li>
 *   <li>Carrega FXML e injeta campos {@code @FXML}</li>
 *   <li>Registra handlers nos botões</li>
 *   <li>Botão clicado → {@code AnnotationRuntime.dispatch()}</li>
 * </ol>
 *
 * <p>O desenvolvedor apenas anota os métodos. O WinterFX faz o resto.</p>
 */
public final class FXMLService {

    private final DiContainer diContainer;

    public FXMLService(DiContainer diContainer) {
        this.diContainer = diContainer;
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    public <T> LoadedView<T> load(ViewDescriptor descriptor, Class<T> controllerType) {
        return loadInternal(descriptor, controllerType, false, null);
    }

    public <T> LoadedView<T> load(ViewDescriptor descriptor, Class<T> controllerType,
                                  Consumer<T> configurator) {
        return loadInternal(descriptor, controllerType, false, configurator);
    }

    public <T> LoadedView<T> loadFresh(ViewDescriptor descriptor, Class<T> controllerType) {
        return loadInternal(descriptor, controllerType, true, null);
    }

    public <T> LoadedView<T> loadFresh(ViewDescriptor descriptor, Class<T> controllerType,
                                       Consumer<T> configurator) {
        return loadInternal(descriptor, controllerType, true, configurator);
    }

    // ==================== INTERNO ====================

    private <T> LoadedView<T> loadInternal(ViewDescriptor descriptor, Class<T> controllerType,
                                           boolean forceFresh, Consumer<T> configurator) {
        try {
            URL fxmlUrl = descriptor.getFxmlUrl();

            Class<?> controllerClass = descriptor.getControllerClass();
            if (controllerClass == null || controllerClass == void.class) {
                controllerClass = controllerType;
            }
            Object diController = diContainer.getBean(controllerClass);

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setController(diController);

            Parent root = loader.load();
            T controller = loader.getController();

            if (controller != null) {
                diContainer.injectDependencies(controller);

                if (configurator != null && controllerType != null
                        && controllerType.isInstance(controller)) {
                    configurator.accept(controllerType.cast(controller));
                }

                registerAllHandlers(root, controller);
            }

            if (controller instanceof RefreshableController refreshable) {
                refreshable.onViewInitialized();
            }

            return new LoadedView<>(root, controller, descriptor.getId(), forceFresh);

        } catch (IOException e) {
            throw new ViewEngineException("Erro: " + descriptor.getId(), e);
        }
    }

    // ==================== REGISTRO DE HANDLERS ====================

    /**
     * Registra UM handler por botão.
     * Quando o botão é clicado, delega para {@link AnnotationRuntime#dispatch}.
     */
    private void registerAllHandlers(Parent root, Object controller) {
        for (Method method : controller.getClass().getDeclaredMethods()) {
            if (!hasRuntimeAnnotations(method)) continue;

            String fxId = method.getName();
            Node node = root.lookup("#" + fxId);

            if (node instanceof ButtonBase button) {
                button.setOnAction(e -> {
                    AnnotationRuntime.dispatch(controller, method.getName(), e);
                });
            }
        }
    }

    /** Verifica se o método possui pelo menos uma anotação runtime. */
    private boolean hasRuntimeAnnotations(Method method) {
        return method.isAnnotationPresent(com.ossobo.winterfx.imagemanager.anotations.SwapImage.class)
                || method.isAnnotationPresent(com.ossobo.winterfx.view.anotations.SwapFxml.class)
                || method.isAnnotationPresent(com.ossobo.winterfx.view.anotations.NewScene.class)
                || method.isAnnotationPresent(com.ossobo.winterfx.notifications.anotations.OnSuccess.class)
                || method.isAnnotationPresent(com.ossobo.winterfx.notifications.anotations.OnError.class)
                || method.isAnnotationPresent(com.ossobo.winterfx.notifications.anotations.OnException.class)
                || method.isAnnotationPresent(com.ossobo.winterfx.notifications.anotations.OnConfirmation.class)
                || method.isAnnotationPresent(com.ossobo.winterfx.notifications.anotations.OnInfo.class)
                || method.isAnnotationPresent(com.ossobo.winterfx.notifications.anotations.OnCritical.class)
                || method.isAnnotationPresent(com.ossobo.winterfx.notifications.anotations.OnWarning.class);
    }
}