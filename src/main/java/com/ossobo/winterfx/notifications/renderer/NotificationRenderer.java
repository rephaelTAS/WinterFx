package com.ossobo.winterfx.notifications.renderer;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.notifications.descriptor.NotificationDescriptor;
import com.ossobo.winterfx.notifications.types.NotificationType;
import com.ossobo.winterfx.resources.api.ResourceAPI;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * NotificationRenderer v1.0
 * Transforma o descriptor em componente visual.
 */
public class NotificationRenderer {

    private final ResourceAPI resourceAPI;
    private final DiContainer container;

    public NotificationRenderer(ResourceAPI resourceAPI, DiContainer container) {
        this.resourceAPI = resourceAPI;
        this.container = container;
    }

    public Object render(NotificationDescriptor descriptor) {
        NotificationType type = descriptor.getType();

        Alert.AlertType alertType = switch (type) {
            case ERROR -> Alert.AlertType.ERROR;
            case WARNING -> Alert.AlertType.WARNING;
            case CONFIRMATION -> Alert.AlertType.CONFIRMATION;
            case SUCCESS, INFO -> Alert.AlertType.INFORMATION;
            default -> Alert.AlertType.NONE;
        };

        Alert alert = new Alert(alertType);

        // Título
        alert.setTitle(descriptor.getTitle() != null ? descriptor.getTitle() : type.name());

        // Cabeçalho (usa description se existir)
        alert.setHeaderText(descriptor.getDescription());

        // Mensagem principal
        alert.setContentText(descriptor.getMessage());

        // Detalhes expandíveis (se existirem)
        if (descriptor.hasDetails()) {
            TextArea textArea = new TextArea(descriptor.getDetails());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            textArea.setPrefRowCount(8);

            alert.getDialogPane().setExpandableContent(new VBox(textArea));
            alert.getDialogPane().setExpanded(true);
        }

        // Botões
        if (descriptor.isYesNo()) {
            ButtonType yesButton = new ButtonType("Sim", ButtonBar.ButtonData.YES);
            ButtonType noButton = new ButtonType("Não", ButtonBar.ButtonData.NO);
            alert.getButtonTypes().setAll(yesButton, noButton);
        }

        return alert;
    }
}
