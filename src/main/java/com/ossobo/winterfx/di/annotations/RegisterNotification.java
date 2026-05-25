package com.ossobo.winterfx.di.annotations;

import com.ossobo.winterfx.di.annotations.enums.NotificationPosition;

import java.lang.annotation.*;

/**
 * Registra uma notificação customizada com FXML.
 *
 * <pre>
 * {@code
 * @RegisterNotification(
 *     id = "notificacao-custom",
 *     fxml = "/fxml/notificacao_custom.fxml",
 *     duration = 5000,
 *     position = NotificationPosition.TOP_RIGHT
 * )
 * public class NotificacaoCustomController { ... }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface RegisterNotification {

    /** ID único da notificação */
    String id();

    /** FXML da notificação (opcional, para layout customizado) */
    String fxml() default "";

    /** Duração padrão em ms */
    long duration() default 3000;

    /** Posição padrão */
    NotificationPosition position() default NotificationPosition.TOP_RIGHT;

    /** Se pode ser fechada manualmente */
    boolean closable() default true;

    /** CSS para estilizar */
    String css() default "";
}