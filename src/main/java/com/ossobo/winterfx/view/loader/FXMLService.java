// FXMLService.java v5.3 - 2026-06-13
// Carrega FXML com controller original, injeta dependências,
// e usa o método execute() da interface WinterFXController para interceptação.
//
// ARQUITETURA HÍBRIDA v5.3:
//   - Controllers (FXML): Interface WinterFXController com método execute()
//     • Requer: implements WinterFXController
//     • @FXML injetado no original ✅
//     • execute() processa anotações via HandlerRegistry ✅
//     • NÃO precisa de proxy JDK! ✅
//     • NÃO precisa de método ponte! ✅
//
//   - Serviços/Repositórios: Proxy ByteBuddy (WinterFXProxyFactory)
//     • Requer: @Service, @Repository
//     • NÃO precisa de interface
//     • @Transactional, @Cacheable funcionam ✅
//
// Vantagens v5.3:
//   - ✅ Sem proxy JDK (evita problemas de interface)
//   - ✅ Código mais simples e direto
//   - ✅ Stack trace limpo
//   - ✅ Integração direta com HandlerRegistry
//   - ✅ Performance excelente
//   - ✅ Sem filtro por nome (apenas ActionEvent)
//   - ✅ Qualquer nome de método é aceito
//   - ✅ 🔥 BUSCA RECURSIVA: encontra botões em TODOS os níveis do FXML
//   - ✅ 🔥 Encontra botões em SplitPane, ScrollPane, TabPane, etc.
//
// @version 5.3 - Busca recursiva completa em todo scene graph
package com.ossobo.winterfx.view.loader;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.runtime.WinterFXProxyFactory;
import com.ossobo.winterfx.view.controller.WinterFXController;
import com.ossobo.winterfx.view.exceptios.ViewEngineException;
import com.ossobo.winterfx.view.refresh.RefreshableController;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tab;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serviço de carregamento FXML com arquitetura híbrida.
 *
 * <p><b>Controllers (FXML):</b></p>
 * <ol>
 *   <li>Obtém controller ORIGINAL do DI (@Controller(proxy=false))</li>
 *   <li>FXMLLoader carrega FXML e injeta campos @FXML no original</li>
 *   <li>Injeta dependências do DI no original</li>
 *   <li>Se implementa WinterFXController, usa execute() para anotações</li>
 *   <li>🔥 Rebinde dos botões para chamar execute()</li>
 *   <li>Retorna LoadedView com controller original (SEM proxy)</li>
 * </ol>
 *
 * <p><b>Serviços/Repositórios:</b> Continuam usando WinterFXProxyFactory (ByteBuddy).</p>
 *
 * <p><b>Regras:</b></p>
 * <ul>
 *   <li>FXML: NÃO usar {@code onAction} — binding é automático pelo fx:id</li>
 *   <li>Controller: Implementar {@code WinterFXController}</li>
 *   <li>Controller: Métodos anotados com @SwapFxml, @OnSuccess, etc.</li>
 *   <li>Controller: NÃO precisa de método ponte!</li>
 *   <li>Método do botão: Deve ter {@code ActionEvent} como parâmetro</li>
 *   <li>fx:id no FXML: Deve ter o MESMO nome do método</li>
 * </ul>
 *
 * @version 5.3 - Interface WinterFXController com método execute(), busca recursiva em todo FXML
 */
public final class FXMLService {

    private static final Logger LOGGER = Logger.getLogger(FXMLService.class.getName());

    private final DiContainer diContainer;
    private final WinterFXProxyFactory serviceProxyFactory; // Para serviços apenas

    public FXMLService(DiContainer diContainer, WinterFXProxyFactory serviceProxyFactory) {
        this.diContainer = diContainer;
        this.serviceProxyFactory = serviceProxyFactory;
    }

    // ==================== API PÚBLICA ====================

    public <T> LoadedView<T> load(ViewDescriptor descriptor, Class<T> controllerType) {
        return loadInternal(descriptor, controllerType, null);
    }

    public <T> LoadedView<T> load(ViewDescriptor descriptor, Class<T> controllerType,
                                  Consumer<T> configurator) {
        return loadInternal(descriptor, controllerType, configurator);
    }

    public <T> LoadedView<T> loadFresh(ViewDescriptor descriptor, Class<T> controllerType) {
        return loadInternal(descriptor, controllerType, null);
    }

    public <T> LoadedView<T> loadFresh(ViewDescriptor descriptor, Class<T> controllerType,
                                       Consumer<T> configurator) {
        return loadInternal(descriptor, controllerType, configurator);
    }

    // ==================== LÓGICA INTERNA ====================

    @SuppressWarnings("unchecked")
    private <T> LoadedView<T> loadInternal(ViewDescriptor descriptor, Class<T> controllerType,
                                           Consumer<T> configurator) {
        try {
            URL fxmlUrl = descriptor.getFxmlUrl();
            Class<?> controllerClass = resolveControllerClass(descriptor, controllerType);

            // 1. Obtém controller ORIGINAL do DI (@Controller(proxy=false))
            Object originalController = diContainer.getBean(controllerClass);

            // 2. Carrega FXML com o ORIGINAL — JavaFX injeta @FXML aqui
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setController(originalController);

            Parent root = loader.load();
            T controller = loader.getController();

            if (controller != null) {
                // 3. Injeta dependências do DI no ORIGINAL
                diContainer.injectDependencies(originalController);

                // 4. Aplica configurador customizado
                if (configurator != null && controllerType != null &&
                        controllerType.isInstance(controller)) {
                    configurator.accept(controllerType.cast(originalController));
                }

                // 5. Notifica RefreshableController
                if (originalController instanceof RefreshableController refreshable) {
                    refreshable.onViewInitialized();
                }

                // 6. 🔥 NÃO cria proxy JDK!
                //    Usa diretamente o controller original com WinterFXController.execute()
                if (originalController instanceof WinterFXController) {
                    LOGGER.info(() -> "✅ Controller implements WinterFXController: " +
                            originalController.getClass().getSimpleName() +
                            " - usando execute() para interceptação");
                } else {
                    LOGGER.info(() -> "ℹ️ Controller sem WinterFXController: " +
                            originalController.getClass().getSimpleName() +
                            " - sem interceptação de anotações");
                }

                // 7. 🔥 Rebinde dos botões (BUSCA RECURSIVA em todo FXML)
                rebindButtons(root, originalController);

                return new LoadedView<>(root, controller, descriptor.getId(), false);
            }

            return new LoadedView<>(root, null, descriptor.getId(), false);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar FXML: " + descriptor.getId(), e);
            throw new ViewEngineException("Erro ao carregar FXML: " + descriptor.getId(), e);
        }
    }

    /**
     * Substitui os handlers de evento dos botões.
     *
     * <p><b>Convenção:</b> O fx:id do botão deve ter o mesmo nome do método.</p>
     * <p><b>Regra:</b> O método DEVE ter {@code ActionEvent} como parâmetro.</p>
     * <p><b>Busca:</b> Percorre TODO o FXML recursivamente, encontrando botões em todos níveis.</p>
     *
     * <p>Exemplo:</p>
     * <pre>
     * &lt;AnchorPane&gt;
     *     &lt;VBox&gt;
     *         &lt;Button fx:id="handleLogin" text="Entrar"/&gt;  ← Botão profundo!
     *     &lt;/VBox&gt;
     * &lt;/AnchorPane&gt;
     *
     * public void handleLogin(ActionEvent event) {
     *     // Código direto - execute() intercepta automaticamente!
     * }
     * </pre>
     */
    private void rebindButtons(Parent root, Object controller) {
        int count = 0;
        boolean isWinterController = controller instanceof WinterFXController;

        LOGGER.info(() -> "🔍 Procurando botões para: " + controller.getClass().getSimpleName());

        for (Method method : controller.getClass().getMethods()) {
            String fxId = method.getName();

            // Pula métodos do Object
            if (isObjectMethod(fxId)) {
                continue;
            }

            // ✅ ÚNICA REGRA: método precisa ter ActionEvent
            if (!hasActionEventParam(method)) {
                LOGGER.fine(() -> "ℹ️ Método " + fxId + " não tem ActionEvent - ignorado");
                continue;
            }

            // 🔥 BUSCA RECURSIVA: encontra botão em TODOS os níveis do FXML
            Node node = findButtonById(root, fxId);

            if (node instanceof ButtonBase button) {
                LOGGER.info(() -> "✅ Botão configurado: " + fxId);

                button.setOnAction(event -> {
                    try {
                        if (isWinterController) {
                            WinterFXController winterController = (WinterFXController) controller;
                            winterController.execute(method.getName(), event);
                        } else {
                            method.invoke(controller, event);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Erro ao invocar handler " + fxId, e);
                    }
                });
                count++;
            } else {
                LOGGER.warning(() -> "⚠️ Botão NÃO encontrado no FXML: " + fxId);
            }
        }

        int finalCount = count;
        LOGGER.info(() -> "✅ " + finalCount + " botões configurados (busca completa em todos níveis)" +
                (isWinterController ? " com WinterFXController.execute()" : ""));
    }

    /**
     * Busca um botão pelo fx:id percorrendo TODO o scene graph recursivamente.
     *
     * <p>Encontra botões em:</p>
     * <ul>
     *   <li>Root direto (nível 1)</li>
     *   <li>Painéis filhos (nível 2+)</li>
     *   <li>SplitPane, ScrollPane, TabPane (filhos especiais)</li>
     *   <li>Múltiplos níveis de profundidade</li>
     * </ul>
     *
     * @param root Painel raiz do FXML
     * @param fxId fx:id do botão procurado
     * @return Node do botão se encontrado, null se não encontrado
     */
    private Node findButtonById(Parent root, String fxId) {
        // 1. Primeiro tenta lookup() (mais rápido para níveis superficiais)
        Node node = root.lookup("#" + fxId);
        if (node != null) {
            return node;
        }

        // 2. Se não encontrar, percorre TODOS os filhos recursivamente
        return findAllButtonsRecursively(root, fxId);
    }

    /**
     * Busca recursiva em TODOS os níveis do scene graph.
     *
     * @param parent Painel atual
     * @param fxId fx:id procurado
     * @return Node encontrado ou null
     */
    private Node findAllButtonsRecursively(Parent parent, String fxId) {
        // Obtém TODOS os filhos (nível atual)
        for (Node child : parent.getChildrenUnmodifiable()) {
            // Verifica se este filho tem o ID procurado
            if (child.getId() != null && child.getId().equals(fxId)) {
                return child;
            }

            // Se o filho é Parent (tem filhos), busca recursivamente (nível +1)
            if (child instanceof Parent childParent) {
                Node found = findAllButtonsRecursively(childParent, fxId);
                if (found != null) {
                    return found;
                }
            }

            // 🔥 CASE SPECIAL: SplitPane tem itens filhos
            if (child instanceof SplitPane splitPane) {
                for (Node splitChild : splitPane.getItems()) {
                    if (splitChild.getId() != null && splitChild.getId().equals(fxId)) {
                        return splitChild;
                    }
                    if (splitChild instanceof Parent splitParent) {
                        Node found = findAllButtonsRecursively(splitParent, fxId);
                        if (found != null) {
                            return found;
                        }
                    }
                }
            }

            // 🔥 CASE SPECIAL: ScrollPane tem conteúdo filho
            if (child instanceof ScrollPane scrollPane) {
                Node content = scrollPane.getContent();
                if (content != null) {
                    if (content.getId() != null && content.getId().equals(fxId)) {
                        return content;
                    }
                    if (content instanceof Parent contentParent) {
                        Node found = findAllButtonsRecursively(contentParent, fxId);
                        if (found != null) {
                            return found;
                        }
                    }
                }
            }

            // 🔥 CASE SPECIAL: TabPane tem tabs com conteúdo
            if (child instanceof TabPane tabPane) {
                for (Tab tab : tabPane.getTabs()) {
                    Node tabContent = tab.getContent();
                    if (tabContent != null) {
                        if (tabContent.getId() != null && tabContent.getId().equals(fxId)) {
                            return tabContent;
                        }
                        if (tabContent instanceof Parent tabParent) {
                            Node found = findAllButtonsRecursively(tabParent, fxId);
                            if (found != null) {
                                return found;
                            }
                        }
                    }
                }
            }
        }

        return null; // Não encontrado
    }

    private boolean isObjectMethod(String name) {
        return name.equals("toString") || name.equals("hashCode") ||
                name.equals("equals") || name.equals("getClass") ||
                name.equals("notify") || name.equals("wait") ||
                name.equals("notifyAll");
    }

    private boolean hasActionEventParam(Method method) {
        for (Class<?> paramType : method.getParameterTypes()) {
            if (paramType == ActionEvent.class) {
                return true;
            }
        }
        return false;
    }

    private Class<?> resolveControllerClass(ViewDescriptor descriptor, Class<?> fallback) {
        Class<?> controllerClass = descriptor.getControllerClass();
        if (controllerClass == null || controllerClass == void.class) {
            return fallback;
        }
        return controllerClass;
    }
}