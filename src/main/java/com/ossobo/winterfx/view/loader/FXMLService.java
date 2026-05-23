package com.ossobo.winterfx.view.loader;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.view.exceptios.ViewEngineException;
import com.ossobo.winterfx.view.refresh.RefreshableController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

/**
 * 🎯 FXML SERVICE - ADAPTADO AO ViewDescriptor CORRETO
 *
 * v2.1 (24/04/2026):
 * - ✅ Integrado FXImageInjector para processamento de @FXImage
 * - ✅ Injeção automática de imagens após carregamento do FXML
 * - ✅ Usa com.ossobo.winterfx.resources.descriptor.ViewDescriptor
 * - ✅ Métodos adaptados: getFxmlUrl(), getId()
 * - ✅ Lógica existente 100% preservada
 */
public final class FXMLService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FXMLService.class);

    private static final ThreadLocal<Boolean> forceFreshLoad = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Consumer<?>> currentConfigurator = new ThreadLocal<>();

    // ==================== API PÚBLICA ====================

    /**
     * ✅ CARREGA FXML PADRÃO (com cache)
     */
    public <T> LoadedView<T> load(ViewDescriptor descriptor, Class<T> controllerType) {
        return loadInternal(descriptor, controllerType, false, null);
    }

    /**
     * ✅ CARREGA FXML COM CONFIGURADOR
     */
    public <T> LoadedView<T> load(ViewDescriptor descriptor, Class<T> controllerType,
                                  Consumer<T> configurator) {
        return loadInternal(descriptor, controllerType, false, configurator);
    }

    /**
     * ✅ CARREGA SEMPRE NOVA INSTÂNCIA (para diálogos)
     */
    public <T> LoadedView<T> loadFresh(ViewDescriptor descriptor, Class<T> controllerType) {
        return loadFresh(descriptor, controllerType, null);
    }

    /**
     * ✅ CARREGA NOVA INSTÂNCIA COM CONFIGURADOR
     */
    public <T> LoadedView<T> loadFresh(ViewDescriptor descriptor, Class<T> controllerType,
                                       Consumer<T> configurator) {
        return loadInternal(descriptor, controllerType, true, configurator);
    }

    // ==================== MÉTODO INTERNO PRINCIPAL ====================

    /**
     * ✅ MÉTODO INTERNO PRINCIPAL (ADAPTADO)
     *
     * Fluxo:
     * 1. Criar FXMLLoader com controller factory
     * 2. Carregar FXML (loader.load())
     * 3. 🎨 Injetar imagens @FXImage (FXImageInjector)
     * 4. Aplicar configurator pós-carregamento
     * 5. Notificar RefreshableController
     * 6. Retornar LoadedView
     */
    private <T> LoadedView<T> loadInternal(ViewDescriptor descriptor, Class<T> controllerType,
                                           boolean forceFresh, Consumer<T> configurator) {
        try {
            // ✅ USA getFxmlUrl() em vez de getFxmlPath()
            URL fxmlUrl = descriptor.getFxmlUrl();
            if (fxmlUrl == null) {
                throw new ViewEngineException("FXML URL é nula para view: " + descriptor.getId());
            }

            LOGGER.debug("📄 Carregando FXML [fresh={}, viewId={}, controllerType={}]",
                    forceFresh, descriptor.getId(),
                    controllerType != null ? controllerType.getSimpleName() : "null");

            // PASSO 1: Configurar e criar loader
            FXMLLoader loader = createLoader(fxmlUrl, forceFresh, controllerType, configurator);

            // PASSO 2: Carregar FXML
            Parent root = loader.load();

            // PASSO 3: Obter controller com type safety
            T controller = getControllerWithTypeSafety(loader, controllerType);

            // PASSO 4: 🎨 INJETAR IMAGENS @FXImage (NOVO!)
            if (controller != null) {
                try {
                    FXImageInjector.injectImages(controller);
                    LOGGER.debug("🎨 @FXImage processado para: {}",
                            controller.getClass().getSimpleName());
                } catch (Exception e) {
                    LOGGER.warn("⚠️ Erro ao processar @FXImage: {}", e.getMessage());
                }
            }

            // PASSO 5: Aplicar configurator pós-carregamento
            if (configurator != null && controller != null && !forceFresh) {
                try {
                    configurator.accept(controller);
                    LOGGER.debug("⚙️ Configurator aplicado ao controller");
                } catch (Exception e) {
                    LOGGER.warn("⚠️ Erro ao aplicar configurator: {}", e.getMessage());
                }
            }

            // PASSO 6: Verificar refreshable e notificar inicialização
            if (controller instanceof RefreshableController) {
                try {
                    RefreshableController refreshable = (RefreshableController) controller;
                    refreshable.onViewInitialized();
                    LOGGER.debug("🔄 RefreshableController.onViewInitialized() chamado");
                } catch (Exception e) {
                    LOGGER.warn("⚠️ Erro ao notificar RefreshableController: {}", e.getMessage());
                }
            }

            // PASSO 7: Criar LoadedView
            LoadedView<T> loadedView = new LoadedView<>(
                    root,
                    controller,
                    descriptor.getId(),
                    forceFresh
            );

            LOGGER.info("✅ FXML carregado [viewId={}, fresh={}, controller={}]",
                    descriptor.getId(), forceFresh,
                    controller != null ? controller.getClass().getSimpleName() : "null");

            return loadedView;

        } catch (IOException e) {
            LOGGER.error("❌ Erro IO ao carregar FXML: {}", descriptor.getId(), e);
            throw new ViewEngineException("Erro ao carregar FXML: " + descriptor.getId(), e);
        } catch (ViewEngineException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("❌ Erro inesperado ao carregar FXML: {}", descriptor.getId(), e);
            throw new ViewEngineException("Erro inesperado ao carregar FXML: " + descriptor.getId(), e);
        }
    }

    // ==================== CRIAÇÃO DE LOADER ====================

    /**
     * ✅ CRIA LOADER INTELIGENTE BASEADO NO MODO
     */
    private <T> FXMLLoader createLoader(URL fxmlUrl, boolean forceFresh,
                                        Class<T> controllerType, Consumer<T> configurator) {
        FXMLLoader loader = new FXMLLoader(fxmlUrl);

        if (forceFresh) {
            // MODO FRESH: Para diálogos, sempre nova instância
            loader.setControllerFactory(param -> {
                try {
                    Object controller = DiContainer.getInstance().getBean(param);
                    applyConfigurator(controller, configurator, controllerType);
                    LOGGER.debug("✅ Novo controller criado via DI para diálogo: {}",
                            param.getSimpleName());
                    return controller;
                } catch (Exception e) {
                    LOGGER.warn("⚠️ DI falhou para diálogo, tentando reflexão: {}", e.getMessage());
                    return createViaReflection(param, configurator, controllerType);
                }
            });
        } else {
            // MODO CACHED: Para views normais, usar DI padrão
            loader.setControllerFactory(param -> {
                try {
                    return DiContainer.getInstance().getBean(param);
                } catch (Exception e) {
                    LOGGER.error("❌ DI falhou no modo cached: {}", e.getMessage());
                    throw new ViewEngineException("Não foi possível criar controller: " +
                            param.getName(), e);
                }
            });
        }

        return loader;
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * ✅ APLICA CONFIGURADOR DE FORMA SEGURA
     */
    @SuppressWarnings("unchecked")
    private <T> void applyConfigurator(Object controller, Consumer<T> configurator,
                                       Class<T> controllerType) {
        if (configurator != null && controller != null && controllerType != null) {
            if (controllerType.isInstance(controller)) {
                try {
                    configurator.accept((T) controller);
                    LOGGER.debug("⚙️ Configurator aplicado ao controller");
                } catch (Exception e) {
                    LOGGER.warn("⚠️ Erro ao aplicar configurator: {}", e.getMessage());
                }
            } else {
                LOGGER.warn("⚠️ Controller não é do tipo esperado: esperado={}, obtido={}",
                        controllerType.getSimpleName(),
                        controller.getClass().getSimpleName());
            }
        }
    }

    /**
     * ✅ CRIA CONTROLLER VIA REFLEXÃO (fallback)
     */
    @SuppressWarnings("unchecked")
    private <T> Object createViaReflection(Class<?> param, Consumer<T> configurator,
                                           Class<T> controllerType) {
        try {
            Object controller = param.getDeclaredConstructor().newInstance();
            applyConfigurator(controller, configurator, controllerType);
            LOGGER.debug("✅ Controller criado via reflexão: {}", param.getSimpleName());
            return controller;
        } catch (Exception e) {
            throw new ViewEngineException("Não foi possível criar controller via reflexão: " +
                    param.getName(), e);
        }
    }

    /**
     * ✅ TYPE SAFETY para controller
     */
    @SuppressWarnings("unchecked")
    private <T> T getControllerWithTypeSafety(FXMLLoader loader, Class<T> controllerType) {
        Object controller = loader.getController();

        if (controller == null) {
            LOGGER.warn("⚠️ Controller é null após carregamento do FXML");
            return null;
        }

        if (controllerType == null || controllerType == Object.class) {
            return (T) controller;
        }

        if (controllerType.isInstance(controller)) {
            return (T) controller;
        } else {
            LOGGER.warn("⚠️ Tipo de controller inesperado: esperado={}, obtido={}",
                    controllerType.getSimpleName(),
                    controller.getClass().getSimpleName());
            return null;
        }
    }

    // ==================== MÉTODOS DE COMPATIBILIDADE ====================

    /**
     * @deprecated Use FXMLManager para métodos de compatibilidade
     */
    @Deprecated
    public <T> LoadedView<T> loadWithConfigurator(String viewId, Consumer<T> configurator) {
        throw new UnsupportedOperationException("Use FXMLManager para métodos de compatibilidade");
    }

    /**
     * Configura o modo fresh load para diálogos
     */
    public static void setupForFreshLoad(Consumer<?> configurator) {
        forceFreshLoad.set(true);
        currentConfigurator.set(configurator);
    }

    /**
     * Limpa o estado após fresh load
     */
    public static void cleanupAfterLoad() {
        forceFreshLoad.remove();
        currentConfigurator.remove();
    }

    /**
     * Verifica se está em modo fresh load
     */
    public static boolean isFreshLoadMode() {
        return Boolean.TRUE.equals(forceFreshLoad.get());
    }
}