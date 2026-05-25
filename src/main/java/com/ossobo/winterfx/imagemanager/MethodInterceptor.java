package com.ossobo.winterfx.imagemanager;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.di.annotations.SwapImage;
import javafx.scene.image.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MethodInterceptor {

    private static final Logger LOGGER = Logger.getLogger(MethodInterceptor.class.getName());

    private MethodInterceptor() {
        throw new UnsupportedOperationException("Classe utilitária");
    }

    public static Object invoke(Object controller, Method method, Object... args) {
        try {
            SwapImage ann = method.getAnnotation(SwapImage.class);

            if (ann != null && ann.before()) {
                swap(controller, ann);
            }

            method.setAccessible(true);
            Object result = method.invoke(controller, args);

            if (ann != null && !ann.before()) {
                swap(controller, ann);
            }

            return result;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao invocar método: " + method.getName(), e);
            throw new RuntimeException(e);
        }
    }

    private static void swap(Object controller, SwapImage ann) {
        try {
            Field field = findField(controller.getClass(), ann.imageView());
            field.setAccessible(true);

            Object value = field.get(controller);
            if (!(value instanceof ImageView imageView)) {
                LOGGER.warning("Campo não é ImageView: " + ann.imageView());
                return;
            }

            ImageManager imageManager = DiContainer.getInstance().getBean(ImageManager.class);
            if (imageManager == null) {
                LOGGER.warning("ImageManager indisponível");
                return;
            }

            if (ann.width() > 0 && ann.height() > 0) {
                imageManager.load(imageView, ann.imageId(), ann.width(), ann.height());
            } else {
                imageManager.load(imageView, ann.imageId());
            }

            LOGGER.fine("Imagem trocada: " + ann.imageId() + " -> " + ann.imageView());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Falha ao trocar imagem", e);
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}