package com.ossobo.winterfx.runtime.handler.before;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;
import com.ossobo.winterfx.view.anotations.SwapFxml;
import com.ossobo.winterfx.view.loader.FXMLService;
import com.ossobo.winterfx.view.loader.LoadedView;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;

import javafx.scene.Parent;
import javafx.scene.layout.Pane;

import java.lang.reflect.Field;

/** Handler para @SwapFxml(before=true) — executado ANTES do método. */
public class SwapFxmlBeforeHandler implements AnnotationHandler<SwapFxml> {

    @Override
    public Class<SwapFxml> getAnnotationType() {
        return SwapFxml.class;
    }

    @Override
    public void handle(AnnotationContext context, SwapFxml annotation) {
        if (!annotation.before()) return;
        swap(context, annotation);
    }

    public static void swap(AnnotationContext context, SwapFxml annotation) {
        try {
            WinterApplication winter = WinterApplication.getInstance();
            if (winter == null) return;
            ResourceRegistry registry = winter.getResourceRegistry();
            if (registry == null) return;

            Field field = findField(context.getTarget().getClass(), annotation.container());
            if (field == null) return;
            field.setAccessible(true);
            Object container = field.get(context.getTarget());
            if (!(container instanceof Pane pane)) return;

            ViewDescriptor descriptor = registry.findViewById(annotation.viewId()).orElse(null);
            if (descriptor == null) return;

            FXMLService fxmlService = winter.getStageManager().getFxmlService();
            LoadedView<?> loadedView = fxmlService.loadFresh(descriptor, Object.class);
            Parent view = loadedView.getRoot();

            pane.getChildren().clear();
            pane.getChildren().add(view);
        } catch (Exception ignored) {}
    }

    static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try { return current.getDeclaredField(name); }
            catch (NoSuchFieldException e) { current = current.getSuperclass(); }
        }
        return null;
    }
}