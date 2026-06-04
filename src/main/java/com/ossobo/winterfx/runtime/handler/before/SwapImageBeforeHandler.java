package com.ossobo.winterfx.runtime.handler.before;

import com.ossobo.winterfx.bootstrap.WinterApplication;
import com.ossobo.winterfx.imagemanager.ImageManager;
import com.ossobo.winterfx.imagemanager.anotations.SwapImage;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;

import javafx.scene.image.ImageView;

import java.lang.reflect.Field;

/** Handler para @SwapImage(before=true) — executado ANTES do método. */
public class SwapImageBeforeHandler implements AnnotationHandler<SwapImage> {

    @Override
    public Class<SwapImage> getAnnotationType() {
        return SwapImage.class;
    }

    @Override
    public void handle(AnnotationContext context, SwapImage annotation) {
        if (!annotation.before()) return;
        swap(context, annotation);
    }

    public static void swap(AnnotationContext context, SwapImage annotation) {
        try {
            WinterApplication winter = WinterApplication.getInstance();
            if (winter == null) return;
            ImageManager imageManager = winter.getImageManager();
            if (imageManager == null) return;

            Field field = findField(context.getTarget().getClass(), annotation.imageView());
            if (field == null) return;
            field.setAccessible(true);
            Object value = field.get(context.getTarget());
            if (!(value instanceof ImageView imageView)) return;

            if (annotation.width() > 0 && annotation.height() > 0) {
                imageManager.load(imageView, annotation.imageId(), annotation.width(), annotation.height());
            } else {
                imageManager.load(imageView, annotation.imageId());
            }
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