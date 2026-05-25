package com.ossobo.winterfx.notifications.controller;

import com.ossobo.winterfx.notifications.model.NotificationInfo;

/**
 * 🎛️ NotificationController v1.0
 *
 * Interface que todo controller FXML de notificação deve implementar.
 * Permite receber os dados do NotificationInfo dinamicamente.
 */
public interface NotificationController {

    /**
     * Recebe os dados completos da notificação.
     * Chamado automaticamente pelo NotificationSender após carregar o FXML.
     *
     * @param info Dados da notificação
     */
    void setNotificationInfo(NotificationInfo info);
}