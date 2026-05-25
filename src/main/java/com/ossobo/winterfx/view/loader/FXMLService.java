package com.ossobo.winterfx.view.loader;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.di.annotations.SwapImage;
import com.ossobo.winterfx.imagemanager.InjectImageInjector;
import com.ossobo.winterfx.imagemanager.MethodInterceptor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.view.exceptios.ViewEngineException;
import com.ossobo.winterfx.view.refresh.RefreshableController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.function.Consumer;

public final class FXMLService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FXMLService.class);

    public <T> LoadedView<T> load(ViewDescriptor descriptor, Class<T> controllerType) {
        return loadInternal(descriptor, controllerType, false, null);
    }

    public <T> LoadedView<T> load(ViewDescriptor descriptor, Class<T> controllerType, Consumer<T> configurator) {
        return loadInternal(descriptor, controllerType, false, configurator);
    }

    public <T> LoadedView<T> loadFresh(ViewDescriptor descriptor, Class<T> controllerType) {
        return loadInternal(descriptor, controllerType, true, null);
    }

    public <T> LoadedView<T> loadFresh(ViewDescriptor descriptor, Class<T> controllerType, Consumer<T> configurator) {
        return loadInternal(descriptor, controllerType, true, configurator);
    }

    private <T> LoadedView<T> loadInternal(ViewDescriptor descriptor, Class<T> controllerType,
                                           boolean forceFresh, Consumer<T> configurator) {
        try {
            URL fxmlUrl = descriptor.getFxmlUrl();
            if (fxmlUrl == null) {
                throw new ViewEngineException("FXML URL é nula: " + descriptor.getId());
            }

            FXMLLoader loader = createLoader(fxmlUrl, controllerType, configurator);
            Parent root = loader.load();
            T controller = getControllerWithTypeSafety(loader, controllerType);

            if (controller != null) {
                try {
                    DiContainer.getInstance().injectDependencies(controller);
                } catch (Exception ignored) {}

                InjectImageInjector.injectImages(controller);
                processControllerAnnotations(controller);
                registerSwapImageHandlers(root, controller);
            }

            if (controller instanceof RefreshableController refreshable) {
                try {
                    refreshable.onViewInitialized();
                } catch (Exception ignored) {}
            }

            LOGGER.info("FXML carregado [viewId={}, controller={}]",
                    descriptor.getId(),
                    controller != null ? controller.getClass().getSimpleName() : "null");

            return new LoadedView<>(root, controller, descriptor.getId(), forceFresh);

        } catch (IOException e) {
            throw new ViewEngineException("Erro IO: " + descriptor.getId(), e);
        } catch (Exception e) {
            throw new ViewEngineException("Erro: " + descriptor.getId(), e);
        }
    }

    private void processControllerAnnotations(Object controller) {
        if (controller == null) return;
        try {
            WinterApplication.getInstance().processBeanAnnotations(controller);
        } catch (Exception e) {
            LOGGER.warn("Erro ao processar anotações: {}", e.getMessage());
        }
    }

    private <T> FXMLLoader createLoader(URL fxmlUrl, Class<T> controllerType, Consumer<T> configurator) {
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setControllerFactory(param -> {
            try {
                Object controller = DiContainer.getInstance().getBean(param);
                DiContainer.getInstance().injectDependencies(controller);

                if (configurator != null && controllerType != null && controllerType.isInstance(controller)) {
                    configurator.accept(controllerType.cast(controller));
                }

                return controller;
            } catch (Exception e) {
                Object c = createViaReflection(param, configurator, controllerType);
                try {
                    DiContainer.getInstance().injectDependencies(c);
                } catch (Exception ignored) {}
                return c;
            }
        });
        return loader;
    }

    private void registerSwapImageHandlers(Parent root, Object controller) {
        Class<?> clazz = controller.getClass();

        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(SwapImage.class)) continue;

            String fxId = method.getName();
            Node node = root.lookup("#" + fxId);

            if (node instanceof ButtonBase button) {
                button.setOnAction(e -> MethodInterceptor.invoke(controller, method, e));
                LOGGER.info("SwapImage ligado: método={}, node=#{}", method.getName(), fxId);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Object createViaReflection(Class<?> param, Consumer<T> configurator, Class<T> controllerType) {
        try {
            Object c = param.getDeclaredConstructor().newInstance();
            if (configurator != null && controllerType != null && controllerType.isInstance(c)) {
                configurator.accept((T) c);
            }
            return c;
        } catch (Exception e) {
            throw new ViewEngineException("Falha: " + param.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getControllerWithTypeSafety(FXMLLoader loader, Class<T> controllerType) {
        Object c = loader.getController();
        if (c == null) return null;
        if (controllerType == null || controllerType == Object.class) return (T) c;
        return controllerType.isInstance(c) ? (T) c : null;
    }
}