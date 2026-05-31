package com.ossobo.winterfx.view.loader;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.imagemanager.MethodInterceptor;
import com.ossobo.winterfx.imagemanager.anotations.SwapImage;
import com.ossobo.winterfx.notifications.NotificationInterceptor;
import com.ossobo.winterfx.notifications.anotations.*;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.anotations.SwapFxml;
import com.ossobo.winterfx.view.exceptios.ViewEngineException;
import com.ossobo.winterfx.view.refresh.RefreshableController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXMLService v4.0
 *
 * Carrega FXML e gerencia TODOS os interceptadores em UM handler por botão.
 * Suporta múltiplas anotações no mesmo método:
 * - @SwapImage + @OnSuccess
 * - @SwapFxml + @OnError
 * - @SwapImage + @SwapFxml + @OnConfirmation
 * - Qualquer combinação!
 *
 * Ordem de execução:
 * 1. @SwapImage (before=true)
 * 2. @SwapFxml (before=true)
 * 3. @OnConfirmation (cancela se Não)
 * 4. Método original
 * 5. @OnSuccess/@OnError/@OnInfo...
 * 6. @SwapFxml (before=false)
 * 7. @SwapImage (before=false)
 */
public final class FXMLService {

    private static final Logger LOGGER = Logger.getLogger(FXMLService.class.getName());

    private final DiContainer diContainer;
    private MethodInterceptor methodInterceptor;
    private NotificationInterceptor notificationInterceptor;
    private ResourceRegistry resourceRegistry;

    public FXMLService(DiContainer diContainer) {
        this.diContainer = diContainer;
    }

    public void setMethodInterceptor(MethodInterceptor methodInterceptor) {
        this.methodInterceptor = methodInterceptor;
    }

    public void setNotificationInterceptor(NotificationInterceptor notificationInterceptor) {
        this.notificationInterceptor = notificationInterceptor;
    }

    public void setResourceRegistry(ResourceRegistry resourceRegistry) {
        this.resourceRegistry = resourceRegistry;
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
            }

            // 🆕 UM único registrador para TODAS as anotações
            if (controller != null) {
                registerAllHandlers(root, controller);
            }

            if (controller instanceof RefreshableController refreshable) {
                refreshable.onViewInitialized();
            }

            LOGGER.log(Level.FINE, "FXML carregado: viewId={0}, controller={1}",
                    new Object[]{descriptor.getId(),
                            controller != null ? controller.getClass().getSimpleName() : "null"});

            return new LoadedView<>(root, controller, descriptor.getId(), forceFresh);

        } catch (IOException e) {
            throw new ViewEngineException("Erro: " + descriptor.getId(), e);
        }
    }

    // ==================== REGISTRADOR ÚNICO ====================

    /**
     * Registra UM handler por botão que executa TODAS as anotações do método.
     * Suporta qualquer combinação de @SwapImage, @SwapFxml, @OnSuccess, @OnError, etc.
     */
    private void registerAllHandlers(Parent root, Object controller) {
        Class<?> clazz = controller.getClass();

        for (Method method : clazz.getDeclaredMethods()) {
            SwapImage swapImage = method.getAnnotation(SwapImage.class);
            SwapFxml swapFxml = method.getAnnotation(SwapFxml.class);
            boolean hasNotify = method.isAnnotationPresent(OnSuccess.class)
                    || method.isAnnotationPresent(OnError.class)
                    || method.isAnnotationPresent(OnException.class)
                    || method.isAnnotationPresent(OnConfirmation.class)
                    || method.isAnnotationPresent(OnInfo.class)
                    || method.isAnnotationPresent(OnCritical.class);

            if (swapImage == null && swapFxml == null && !hasNotify) continue;

            String fxId = method.getName();
            Node node = root.lookup("#" + fxId);

            if (node instanceof ButtonBase button) {
                button.setOnAction(e -> executeAllInterceptors(
                        controller, method, e, swapImage, swapFxml, hasNotify));
                LOGGER.log(Level.FINE, "Handler registrado: método={0}, node=#{1}",
                        new Object[]{method.getName(), fxId});
            }
        }
    }

    /**
     * Executa todos os interceptadores para um método, na ordem correta.
     */
    private void executeAllInterceptors(Object controller, Method method, Object event,
                                        SwapImage swapImage, SwapFxml swapFxml, boolean hasNotify) {
        try {
            // ========== 1. @SwapImage BEFORE ==========
            if (swapImage != null && swapImage.before() && methodInterceptor != null) {
                methodInterceptor.invokeSwap(controller, swapImage);
            }

            // ========== 2. @SwapFxml BEFORE ==========
            if (swapFxml != null && swapFxml.before()) {
                swapFxml(controller, swapFxml);
            }

            // ========== 3. @OnConfirmation ==========
            if (hasNotify && notificationInterceptor != null) {
                OnConfirmation conf = method.getAnnotation(OnConfirmation.class);
                if (conf != null && !notificationInterceptor.processBefore(method)) {
                    return; // Usuário cancelou
                }
            }

            // ========== 4. EXECUTA O MÉTODO ==========
            method.setAccessible(true);
            Exception methodError = null;
            try {
                if (method.getParameterCount() == 0) {
                    method.invoke(controller);
                } else {
                    method.invoke(controller, event);
                }
            } catch (Exception ex) {
                methodError = ex;
            }

            // ========== 5. NOTIFICAÇÕES (@OnSuccess/@OnError...) ==========
            if (hasNotify && notificationInterceptor != null) {
                notificationInterceptor.processAfter(method, methodError);
            }

            // ========== 6. @SwapFxml AFTER ==========
            if (swapFxml != null && !swapFxml.before()) {
                swapFxml(controller, swapFxml);
            }

            // ========== 7. @SwapImage AFTER ==========
            if (swapImage != null && !swapImage.before() && methodInterceptor != null) {
                methodInterceptor.invokeSwap(controller, swapImage);
            }

            // Relança exceção se o método falhou
            if (methodError != null) {
                throw methodError;
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Erro ao executar handlers: " + method.getName(), ex);
        }
    }

    // ==================== @SwapFxml ====================

    private void swapFxml(Object controller, SwapFxml ann) {
        try {
            Field field = findField(controller.getClass(), ann.container());
            field.setAccessible(true);
            Object container = field.get(controller);

            if (container == null) {
                LOGGER.warning("Container não encontrado: " + ann.container());
                return;
            }

            ViewDescriptor descriptor = resourceRegistry.findViewById(ann.viewId())
                    .orElseThrow(() -> new IllegalArgumentException("View não registrada: " + ann.viewId()));

            LoadedView<?> loadedView = loadFresh(descriptor, Object.class);
            Parent view = loadedView.getRoot();

            if (container instanceof Pane pane) {
                pane.getChildren().clear();
                pane.getChildren().add(view);
                LOGGER.fine("FXML trocado: " + ann.viewId() + " -> " + ann.container());
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Falha ao trocar FXML: " + ann.viewId(), e);
        }
    }

    // ==================== UTILITÁRIO ====================

    private Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}