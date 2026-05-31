package com.ossobo.winterfx.imagemanager;

import com.ossobo.winterfx.imagemanager.anotations.SwapImage;

import javafx.scene.image.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MethodInterceptor v1.1
 *
 * Interceptador de métodos anotados com @SwapImage.
 * Executa o método e troca a imagem antes ou depois.
 */
public final class MethodInterceptor {

    private static final Logger LOGGER = Logger.getLogger(MethodInterceptor.class.getName());

    private final ImageManager imageManager;

    public MethodInterceptor(ImageManager imageManager) {
        this.imageManager = imageManager;
    }

    /**
     * Invoca um método com interceptação @SwapImage.
     *
     * @param controller instância do controller
     * @param method     método a ser invocado
     * @param args       argumentos opcionais do método
     * @return resultado da invocação
     */
    public Object invoke(Object controller, Method method, Object... args) {
        try {
            SwapImage ann = method.getAnnotation(SwapImage.class);

            // Swap antes da execução
            if (ann != null && ann.before()) {
                swap(controller, ann);
            }

            // Executa o método
            method.setAccessible(true);
            Object result;
            if (method.getParameterCount() == 0) {
                result = method.invoke(controller);      // sem argumentos
            } else {
                result = method.invoke(controller, args); // com argumentos
            }

            // Swap depois da execução
            if (ann != null && !ann.before()) {
                swap(controller, ann);
            }

            return result;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao invocar método: " + method.getName(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Troca a imagem no ImageView conforme anotação.
     */
    private void swap(Object controller, SwapImage ann) {
        try {
            Field field = findField(controller.getClass(), ann.imageView());
            field.setAccessible(true);

            Object value = field.get(controller);
            if (!(value instanceof ImageView imageView)) {
                LOGGER.warning("Campo não é ImageView: " + ann.imageView());
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

    /**
     * Apenas executa o swap, sem invocar o método.
     * Usado pelo FXMLService quando múltiplas anotações estão no mesmo método.
     */
    public void invokeSwap(Object controller, SwapImage ann) {
        swap(controller, ann);
    }

    /**
     * Encontra um campo subindo na hierarquia de classes.
     */
    private Field findField(Class<?> type, String name) throws NoSuchFieldException {
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