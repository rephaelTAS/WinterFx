package com.ossobo.winterfx.view.controller;

import com.ossobo.winterfx.di.injection.DependencyInjector;
import com.ossobo.winterfx.scanner.ReflectionScanner;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.anotations.GetController;

import java.lang.reflect.Field;
import java.util.List;

/**
 * GetControllerInjector — Injeta controllers APÓS o FXML ser carregado.
 *
 * Diferente do @Inject (que obtém o bean antes do FXML),
 * este injector chama stageManager.loadView() antes de injetar,
 * garantindo que @FXML estejam disponíveis.
 */
public class GetControllerInjector implements DependencyInjector {

    private final ReflectionScanner reflectionScanner;
    private final StageManager stageManager;
    private final ResourceRegistry resourceRegistry;

    public GetControllerInjector(ReflectionScanner reflectionScanner,
                                 StageManager stageManager,
                                 ResourceRegistry resourceRegistry) {
        this.reflectionScanner = reflectionScanner;
        this.stageManager = stageManager;
        this.resourceRegistry = resourceRegistry;
    }

    @Override
    public void inject(Object instance, Class<?> type) {
        List<Field> fields = reflectionScanner.getFieldsWithAnnotation(type, GetController.class);

        for (Field field : fields) {
            try {
                Class<?> controllerType = field.getType();

                // Descobre o viewId pelo tipo do controller no ResourceRegistry
                String viewId = resourceRegistry.findAllViews().stream()
                        .filter(v -> v.getControllerClass() != null
                                && v.getControllerClass().equals(controllerType))
                        .map(v -> v.getId())
                        .findFirst()
                        .orElse(null);

                if (viewId == null) {
                    throw new IllegalArgumentException(
                            "Nenhuma view registrada para o controller: " + controllerType.getName());
                }

                // Carrega a view (garante que @FXML foi injetado)
                var loadedView = stageManager.loadView(viewId);
                Object controller = loadedView.getController();

                // Injeta o controller que JÁ passou pelo FXMLLoader
                field.setAccessible(true);
                field.set(instance, controller);

            } catch (Exception e) {
                throw new RuntimeException("Falha ao injetar @GetController: " + field.getName(), e);
            }
        }
    }
}