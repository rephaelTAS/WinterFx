package com.ossobo.winterfx.di.annotations;

import java.lang.annotation.*;

/**
 * Injeta um NotificationSender para disparar notificações programaticamente.
 *
 * <pre>
 * {@code
 * @NotifySender
 * private NotificationSender sender;
 *
 * public void fazerAlgo() {
 *     sender.success.fxml("Operação concluída!");
 *     sender.error("Falha na operação!");
 *     sender.warning("Atenção!");
 *     sender.info("Informação importante");
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface NotifySender {
}