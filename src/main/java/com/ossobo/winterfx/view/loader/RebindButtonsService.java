package com.ossobo.winterfx.view.loader;

import com.ossobo.winterfx.view.controller.WinterFXController;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.lang.System.Logger;

/**
 * RebindButtonsService v4.0 - Híbrido (Namespace + Lookup)
 *
 * <p><b>Estratégia:</b></p>
 * <ul>
 *   <li><b>1º (O(1)):</b> Busca no namespace do FXML principal</li>
 *   <li><b>2º (O(n) nativo):</b> Busca via root.lookup() para FXMLs incluídos</li>
 * </ul>
 *
 * <p><b>Pré-requisito para FXMLs incluídos (fx:include):</b></p>
 * <ul>
 *   <li>O botão deve ter {@code fx:id="nomeBotao"} E {@code id="nomeBotao"}</li>
 *   <li>Exemplo: {@code <Button fx:id="salvar" id="salvar" />}</li>
 * </ul>
 *
 * @version 4.0 (25/08/2026)
 */
public final class RebindButtonsService {

    private static final Logger LOGGER = System.getLogger(RebindButtonsService.class.getName());

    /**
     * Vincula os botões do FXML aos métodos do controller.
     *
     * @param namespace Mapeamento fx:id → Node (do FXMLLoader.getNamespace())
     * @param root Raiz da view (necessária para lookup em fx:include)
     * @param controller Controller que contém os métodos
     * @return Número de botões vinculados
     */
    public int bind(Map<String, Object> namespace, Parent root, Object controller) {
        if (controller == null || root == null) {
            LOGGER.log(Logger.Level.WARNING, "Controller ou root nulo - não é possível vincular botões");
            return 0;
        }

        int count = 0;
        boolean isWinterController = controller instanceof WinterFXController;

        // ✅ Itera sobre os métodos do controller (fonte única de verdade)
        for (Method method : controller.getClass().getMethods()) {
            String fxId = method.getName();

            // Ignora métodos do Object
            if (isObjectMethod(fxId)) continue;

            // Verifica se o método aceita ActionEvent
            if (!hasActionEventParam(method)) continue;

            ButtonBase button = null;

            // ✅ 1ª TENTATIVA (O(1)): Busca no namespace do FXML principal
            // Funciona com fx:id (não precisa do atributo id)
            if (namespace != null) {
                Object obj = namespace.get(fxId);
                if (obj instanceof ButtonBase b) {
                    button = b;
                }
            }

            // ✅ 2ª TENTATIVA (O(n) nativo): Busca via lookup para fx:include
            // REQUER: id="nomeBotao" no FXML filho (além do fx:id)
            if (button == null) {
                Node foundNode = root.lookup("#" + fxId);
                if (foundNode instanceof ButtonBase b) {
                    button = b;
                }
            }

            // ✅ Vincula se encontrou um botão
            if (button != null) {
                button.setOnAction(event -> {
                    try {
                        if (isWinterController) {
                            ((WinterFXController) controller).execute(method.getName(), event);
                        } else {
                            if (!method.canAccess(controller)) {
                                method.setAccessible(true);
                            }
                            method.invoke(controller, event);
                        }
                        LOGGER.log(Logger.Level.DEBUG, "🎯 Botão executado: {0}", fxId);
                    } catch (Exception e) {
                        LOGGER.log(Logger.Level.ERROR, "Erro ao executar ação do botão: " + fxId, e);
                    }
                });
                count++;
            }
        }

        if (count > 0) {
            LOGGER.log(Logger.Level.DEBUG, "🔗 {0} botões vinculados (namespace + lookup)", count);
        } else {
            LOGGER.log(Logger.Level.DEBUG, "⚠️ Nenhum botão vinculado para: {0}",
                    controller.getClass().getSimpleName());
        }

        return count;
    }

    /**
     * Versão para uso direto do cache, repassando o root armazenado.
     */
    public int bindFromCache(String viewId, Map<String, Object> namespace, Parent root, Object controller) {
        int count = bind(namespace, root, controller);
        if (count > 0) {
            LOGGER.log(Logger.Level.DEBUG, "🔄 Botões vinculados do cache (namespace + lookup) para: {0}", viewId);
        }
        return count;
    }

    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================

    private boolean isObjectMethod(String name) {
        return Arrays.asList("toString", "hashCode", "equals", "getClass",
                "notify", "wait", "notifyAll").contains(name);
    }

    private boolean hasActionEventParam(Method method) {
        return Arrays.stream(method.getParameterTypes())
                .anyMatch(paramType -> paramType == ActionEvent.class);
    }
}