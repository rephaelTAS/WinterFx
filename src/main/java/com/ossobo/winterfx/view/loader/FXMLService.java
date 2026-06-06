package com.ossobo.winterfx.view.loader;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.runtime.AnnotationRuntime;
import com.ossobo.winterfx.runtime.MethodResolutionException;
import com.ossobo.winterfx.runtime.ResolutionStatus;
import com.ossobo.winterfx.view.exceptios.ViewEngineException;
import com.ossobo.winterfx.view.refresh.RefreshableController;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FXMLService {

    private static final Logger LOGGER = Logger.getLogger(FXMLService.class.getName());

    private final DiContainer diContainer;

    public FXMLService(DiContainer diContainer) {
        this.diContainer = diContainer;
    }

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

                if (configurator != null && controllerType != null && controllerType.isInstance(controller)) {
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

    private void registerAllHandlers(Parent root, Object controller) {
        boolean isProxy = controller.getClass().getName().contains("ByteBuddy")
                || Proxy.isProxyClass(controller.getClass());

        for (Method method : controller.getClass().getDeclaredMethods()) {
            if (!hasRuntimeAnnotations(method)) continue;

            String fxId = method.getName();
            Node node = root.lookup("#" + fxId);

            if (!(node instanceof ButtonBase button)) continue;

            button.setOnAction(event -> {
                try {
                    if (isProxy) {
                        tryInvokeControllerMethod(controller, method, event);
                    } else {
                        dispatchWithFallback(controller, method.getName(), event);
                    }
                } catch (MethodResolutionException ex) {
                    throw new RuntimeException(buildHandlerErrorMessage(method.getName()) + ": " + ex.getMessage(), ex);
                } catch (Exception ex) {
                    throw new RuntimeException(buildHandlerErrorMessage(method.getName()), ex);
                }
            });
        }
    }

    private void dispatchWithFallback(Object controller, String methodName, ActionEvent event) {
        try {
            AnnotationRuntime.dispatch(controller, methodName, event);
        } catch (MethodResolutionException firstFailure) {
            if (!firstFailure.isSignatureMismatch()) {
                throw firstFailure;
            }

            try {
                AnnotationRuntime.dispatch(controller, methodName);
                LOGGER.log(Level.WARNING,
                        "FXMLService: fallback sem ActionEvent usado para {0}. Considere ajustar a assinatura.",
                        methodName);
            } catch (MethodResolutionException secondFailure) {
                throw new IllegalStateException(
                        "Falha nas duas tentativas para " + methodName + ": [com evento] "
                                + firstFailure.getMessage() + " | [sem evento] " + secondFailure.getMessage(),
                        secondFailure
                );
            }
        }
    }

    private void tryInvokeControllerMethod(Object controller, Method method, ActionEvent event) throws Exception {
        method.setAccessible(true);

        Class<?>[] params = method.getParameterTypes();
        if (params.length == 0) {
            method.invoke(controller);
            return;
        }

        if (params.length == 1 && event != null && params[0].isAssignableFrom(event.getClass())) {
            method.invoke(controller, event);
            return;
        }

        throw new IllegalArgumentException("Assinatura incompatível para " + method.getName());
    }

    private String buildHandlerErrorMessage(String methodName) {
        return "Erro ao invocar handler " + methodName + " via FXMLService";
    }

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