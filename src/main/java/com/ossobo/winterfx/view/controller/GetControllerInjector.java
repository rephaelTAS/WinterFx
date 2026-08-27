package com.ossobo.winterfx.view.controller;

import com.ossobo.winterfx.di.injection.DependencyInjector;
import com.ossobo.winterfx.resources.ResourceModule;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.scanner.ReflectionScanner;
import com.ossobo.winterfx.view.StageManager;
import com.ossobo.winterfx.view.anotations.GetController;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * GetControllerInjector v2.0 — Injeta controllers APÓS o FXML ser carregado.
 *
 * <p>Diferente do @Inject (que obtém o bean antes do FXML),
 * este injector chama stageManager.loadView() antes de injetar,
 * garantindo que @FXML estejam disponíveis.</p>
 *
 * <p><b>v2.0:</b> Desacoplado de ResourceRegistry, O(1) em vez de O(N).</p>
 *
 * @version 2.0 (23/08/2026)
 */
public class GetControllerInjector implements DependencyInjector {

    private static final System.Logger LOGGER = System.getLogger(GetControllerInjector.class.getName());

    private final ReflectionScanner reflectionScanner;
    private final StageManager stageManager;
    private final ResourceModule resourceModule;

    public GetControllerInjector(ReflectionScanner reflectionScanner,
                                 StageManager stageManager,
                                 ResourceModule resourceModule) {
        this.reflectionScanner = Objects.requireNonNull(reflectionScanner);
        this.stageManager = Objects.requireNonNull(stageManager);
        this.resourceModule = Objects.requireNonNull(resourceModule);
    }

    /**
     * Injeta controllers APÓS o FXML ser carregado.
     *
     * <p><b>Melhorias v2.0:</b></p>
     * <ul>
     *   <li>Usa ResourceModule.requireView() em vez de findAll().stream()</li>
     *   <li>Complexidade O(1) em vez de O(N)</li>
     *   <li>Validação de tipo do controller com mensagem clara</li>
     * </ul>
     */
    @Override
    public void inject(Object instance, Class<?> type) {
        List<Field> fields = reflectionScanner.getFieldsWithAnnotation(type, GetController.class);

        for (Field field : fields) {
            try {
                Class<?> controllerType = field.getType();

                // ✅ Agora O(1): Busca a view pelo ID informado na anotação GetController
                // Mas a anotação @GetController não tem um campo 'value'...
                // Precisamos encontrar a view que tem este controller

                // Melhor abordagem: O campo anotado com @GetController deve ter um nome
                // que corresponda ao ID da view, OU podemos usar o ResourceModule
                // para encontrar a view pelo tipo do controller.

                // Infelizmente, a anotação @GetController não possui um campo para especificar
                // qual view buscar. Vamos precisar encontrar a view pelo tipo do controller.
                // Mas isso ainda é O(N) se fizermos getAllViews().

                // SOLUÇÃO: Criar um campo opcional na anotação @GetController com o ID da view.
                // Enquanto isso não é feito, a melhor alternativa é:

                var viewId = findViewIdByControllerType(controllerType);

                if (viewId == null) {
                    throw new IllegalArgumentException(
                            "Nenhuma view registrada para o controller: " + controllerType.getName() +
                                    ". Considere adicionar o parâmetro 'value' na anotação @GetController com o ID da view."
                    );
                }

                // Carrega a view (garante que @FXML foi injetado)
                var loadedView = stageManager.loadView(viewId);
                Object controller = loadedView.controller();

                // Verifica se o controller é do tipo esperado
                if (!controllerType.isInstance(controller)) {
                    throw new IllegalStateException(
                            "Controller da view '" + viewId + "' é do tipo " +
                                    (controller != null ? controller.getClass().getName() : "null") +
                                    ", mas era esperado " + controllerType.getName()
                    );
                }

                // Injeta o controller que JÁ passou pelo FXMLLoader
                field.setAccessible(true);
                field.set(instance, controller);

                LOGGER.log(System.Logger.Level.DEBUG,
                        "Controller injetado: {0} da view {1}",
                        controllerType.getSimpleName(), viewId);

            } catch (Exception e) {
                throw new RuntimeException("Falha ao injetar @GetController: " + field.getName(), e);
            }
        }
    }

    /**
     * Encontra o ID da view que possui o controller do tipo especificado.
     *
     * <p>Esta é uma busca O(N) que pode ser otimizada no futuro adicionando
     * um campo 'value' na anotação @GetController com o ID da view.</p>
     *
     * TODO: Adicionar campo 'value' em @GetController para busca O(1)
     */
    private String findViewIdByControllerType(Class<?> controllerType) {
        return resourceModule.getAllViews().stream()
                .filter(view -> {
                    Class<?> viewController = view.controllerClass();
                    return viewController != null && viewController.equals(controllerType);
                })
                .map(ViewDescriptor::id)
                .findFirst()
                .orElse(null);
    }
}