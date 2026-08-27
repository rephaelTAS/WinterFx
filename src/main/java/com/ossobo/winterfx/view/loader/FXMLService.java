package com.ossobo.winterfx.view.loader;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.view.exceptios.ViewEngineException;
import com.ossobo.winterfx.view.injection.ReactiveViewInjector;
import com.ossobo.winterfx.view.injection.ViewState;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Map;
import java.lang.System.Logger;

/**
 * FXMLService v12.0 - Responsável APENAS por carregar FXML e extrair namespace.
 *
 * <p><b>Responsabilidades:</b></p>
 * <ul>
 *   <li>Carregar o FXML usando FXMLLoader</li>
 *   <li>Obter controller do DI</li>
 *   <li>Extrair namespace (fx:id → Node) do FXMLLoader</li>
 *   <li>Aplicar MVVM (ReactiveViewInjector)</li>
 *   <li>Retornar LoadResult (root, namespace, controller, viewState)</li>
 * </ul>
 *
 * <p><b>NÃO FAZ:</b></p>
 * <ul>
 *   <li>❌ Aplicar CSS (StyleManager faz)</li>
 *   <li>❌ Vincular botões (RebindButtonsService faz)</li>
 *   <li>❌ Gerenciar cache (StageManager faz)</li>
 * </ul>
 *
 * @version 12.0 (25/08/2026)
 */
public final class FXMLService {

    private static final Logger LOGGER = System.getLogger(FXMLService.class.getName());

    private final DiContainer diContainer;

    public FXMLService(DiContainer diContainer) {
        this.diContainer = diContainer;
    }

    /**
     * Carrega o FXML e retorna todos os componentes necessários.
     *
     * @param descriptor Descritor da view
     * @param controllerType Tipo do controller (para fallback)
     * @return LoadResult contendo root, namespace, controller e viewState
     */
    public LoadResult load(ViewDescriptor descriptor, Class<?> controllerType) {
        try {
            var fxmlUrl = descriptor.fxmlUrl();
            var controllerClass = resolveControllerClass(descriptor, controllerType);

            // FASE 1: Obtém controller do DI
            var controller = diContainer.getBean(controllerClass);
            LOGGER.log(Logger.Level.DEBUG, "📦 Controller obtido do DI: {0}",
                    controllerClass.getSimpleName());

            // FASE 2: Carrega o FXML
            var loader = new FXMLLoader(fxmlUrl);
            loader.setController(controller);
            Parent root = loader.load();

            // FASE 3: Extrai o namespace (fx:id → Node)
            Map<String, Object> namespace = loader.getNamespace();
            LOGGER.log(Logger.Level.DEBUG, "📋 Namespace capturado com {0} elementos",
                    namespace.size());

            // FASE 4: Motor MVVM invisível
            var viewState = new ViewState();
            var reactiveInjector = new ReactiveViewInjector(viewState);
            reactiveInjector.injectReactiveState(controller, root);

            LOGGER.log(Logger.Level.DEBUG, "✅ FXML carregado: {0} com controller {1}",
                    descriptor.id(), controllerClass.getSimpleName());

            // Retorna TUDO: root, namespace, controller, viewState
            return new LoadResult(root, namespace, controller, viewState);

        } catch (IOException e) {
            LOGGER.log(Logger.Level.ERROR, "Erro ao carregar FXML: " + descriptor.id(), e);
            throw new ViewEngineException("Erro ao carregar FXML: " + descriptor.id(), e);
        }
    }

    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================

    private Class<?> resolveControllerClass(ViewDescriptor descriptor, Class<?> fallback) {
        var controllerClass = descriptor.controllerClass();
        return (controllerClass == null || controllerClass == void.class) ? fallback : controllerClass;
    }

    /**
     * Resultado do load: root, namespace, controller e viewState.
     */
    public record LoadResult(
            Parent root,
            Map<String, Object> namespace,
            Object controller,
            ViewState viewState
    ) {
        public LoadResult {
            if (root == null) throw new IllegalArgumentException("root não pode ser nulo");
            if (namespace == null) throw new IllegalArgumentException("namespace não pode ser nulo");
        }

        public boolean hasController() {
            return controller != null;
        }

        @SuppressWarnings("unchecked")
        public <T> T getControllerAs(Class<T> type) {
            if (hasController() && type.isInstance(controller)) {
                return (T) controller;
            }
            throw new ClassCastException("Controller não é do tipo " + type.getSimpleName());
        }
    }
}