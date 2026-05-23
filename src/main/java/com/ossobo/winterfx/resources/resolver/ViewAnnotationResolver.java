package com.ossobo.winterfx.resources.resolver;

import com.ossobo.winterfx.di.annotations.RegisterView;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor.*;
import com.ossobo.winterfx.resources.enums.ResourceOrigin;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ViewAnnotationResolver v2.0
 *
 * Resolve anotações @RegisterView em ViewDescriptor.
 * Faz a ponte entre os caminhos String da anotação e os URLs usados pelo ViewDescriptor.
 */
public final class ViewAnnotationResolver {

    private ViewAnnotationResolver() {}

    /**
     * Constrói um ViewDescriptor a partir de uma classe anotada com @RegisterView.
     */
    public static ViewDescriptor resolve(Class<?> annotatedClass) {
        RegisterView ann = annotatedClass.getAnnotation(RegisterView.class);

        if (ann == null) {
            throw new IllegalArgumentException(
                    "Classe não anotada com @RegisterView: " + annotatedClass.getName()
            );
        }

        return resolve(ann, annotatedClass);
    }

    /**
     * Constrói um ViewDescriptor a partir da anotação.
     */
    public static ViewDescriptor resolve(RegisterView ann, Class<?> fallbackController) {
        // Resolve FXML para URL (obrigatório)
        URL fxmlUrl = resolveUrl(ann.fxml(), "FXML", ann.id(), true);

        // Define controller class
        Class<?> controllerClass = ann.controllerClass() == void.class
                ? fallbackController
                : ann.controllerClass();

        // Constrói o builder
        ViewDescriptor.Builder builder = ViewDescriptor.builder()
                .id(ann.id())
                .fxmlUrl(fxmlUrl)
                .origin(ann.origin())
                .controllerClass(controllerClass)
                .viewType(ann.viewType())
                .cssMode(ann.cssMode())
                .modeUse(ann.modeUse())
                .description(ann.description())
                .tags(ann.tags().length > 0 ? List.of(ann.tags()) : null)
                .encoding(ann.encoding())
                .initMethod(ann.initMethod())
                .eager(ann.eager())
                .loadOrder(ann.loadOrder());

        // CSS primário
        if (!ann.primaryCss().isEmpty()) {
            URL primaryCssUrl = resolveUrl(ann.primaryCss(), "CSS primário", ann.id(), false);
            if (primaryCssUrl != null) {
                builder.primaryCss(primaryCssUrl);
            }
        }

        // CSS adicionais
        if (ann.additionalCss().length > 0) {
            List<URL> cssUrls = new ArrayList<>();
            for (String cssPath : ann.additionalCss()) {
                URL cssUrl = resolveUrl(cssPath, "CSS adicional", ann.id(), false);
                if (cssUrl != null) {
                    cssUrls.add(cssUrl);
                }
            }
            if (!cssUrls.isEmpty()) {
                builder.additionalCss(Collections.unmodifiableList(cssUrls));
            }
        }

        // Style classes
        if (ann.styleClasses().length > 0) {
            builder.styleClasses(List.of(ann.styleClasses()));
        }

        // Stage
        builder.title(ann.title())
                .width(ann.width())
                .height(ann.height())
                .resizable(ann.resizable())
                .centered(ann.centered())
                .alwaysOnTop(ann.alwaysOnTop())
                .stageStyle(ann.stageStyle());

        // Ícone
        if (!ann.icon().isEmpty()) {
            URL iconUrl = resolveUrl(ann.icon(), "Ícone", ann.id(), false);
            if (iconUrl != null) {
                builder.iconUrl(iconUrl);
            }
        }

        // Resource bundle
        if (!ann.resourceBundle().isEmpty()) {
            builder.resourceBundle(ann.resourceBundle());
        }

        // Alerta
        if (ann.modeUse() == ModeUse.ALERT) {
            builder.alertType(ann.alertType())
                    .modality(ann.modality())
                    .confirmationRequired(ann.confirmationRequired())
                    .autoCloseMillis(ann.autoCloseMillis())
                    .confirmText(ann.confirmText())
                    .cancelText(ann.cancelText());

            if (!ann.sound().isEmpty()) {
                URL soundUrl = resolveUrl(ann.sound(), "Som", ann.id(), false);
                if (soundUrl != null) {
                    builder.soundUrl(soundUrl);
                }
            }

            if (!ann.alertIcon().isEmpty()) {
                URL alertIconUrl = resolveUrl(ann.alertIcon(), "Ícone alerta", ann.id(), false);
                if (alertIconUrl != null) {
                    builder.iconUrl(alertIconUrl);
                }
            }
        }

        return builder.build();
    }

    /**
     * Resolve um caminho para URL via classpath.
     */
    private static URL resolveUrl(String path, String tipo, String viewId, boolean required) {
        URL url = ViewAnnotationResolver.class.getResource(path);

        if (url == null) {
            String mensagem = String.format(
                    "%s não encontrado para view '%s': %s", tipo, viewId, path
            );

            if (required) {
                throw new IllegalArgumentException(mensagem);
            } else {
                System.err.println("⚠️ " + mensagem);
                return null;
            }
        }

        return url;
    }
}