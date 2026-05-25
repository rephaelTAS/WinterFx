package com.ossobo.winterfx.imagemanager;

import com.ossobo.winterfx.di.DiContainer;
import com.ossobo.winterfx.di.annotations.InjectImage;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 🖼️ InjectImageInjector - Processa a anotação @InjectImage
 *
 * Injeta automaticamente imagens nos campos ImageView anotados.
 * Chamado automaticamente pelo FXMLService após carregar cada view FXML.
 *
 * <p>Uso no Controller:</p>
 * <pre>
 * {@code @FXML @InjectImage("logo")}
 * private ImageView logoView;
 * </pre>
 *
 * @author WinterFX
 * @version 1.0
 */
public final class InjectImageInjector {

    private static final Logger LOGGER = Logger.getLogger(InjectImageInjector.class.getName());

    private InjectImageInjector() {
        throw new UnsupportedOperationException("Classe utilitária - não instanciar");
    }

    /**
     * Processa todas as anotações @InjectImage em um controller.
     * Chamado automaticamente pelo FXMLService após o FXMLLoader.load().
     *
     * @param controller Instância do controller FXML (já com @FXML populados)
     */
    public static void injectImages(Object controller) {
        if (controller == null) {
            LOGGER.warning("⚠️ Controller nulo, ignorando injeção de imagens");
            return;
        }

        Class<?> clazz = controller.getClass();
        LOGGER.fine("🔍 Processando @InjectImage em: " + clazz.getSimpleName());

        int injectedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        for (Field field : clazz.getDeclaredFields()) {
            InjectImage ann = field.getAnnotation(InjectImage.class);

            if (ann == null) continue;

            // Verifica se o campo é do tipo ImageView
            if (!ImageView.class.isAssignableFrom(field.getType())) {
                LOGGER.warning("⚠️ @InjectImage em campo não-ImageView: " +
                        clazz.getSimpleName() + "." + field.getName());
                skippedCount++;
                continue;
            }

            try {
                field.setAccessible(true);
                ImageView imageView = (ImageView) field.get(controller);

                if (imageView == null) {
                    LOGGER.warning("⚠️ ImageView nulo: " + clazz.getSimpleName() + "." + field.getName());
                    skippedCount++;
                    continue;
                }

                boolean success = injectImage(imageView, ann);
                if (success) injectedCount++;
                else errorCount++;

            } catch (IllegalAccessException e) {
                LOGGER.log(Level.SEVERE, "❌ Erro ao acessar campo: " + field.getName(), e);
                errorCount++;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "❌ Erro inesperado: " + field.getName(), e);
                errorCount++;
            }
        }

        if (injectedCount > 0 || errorCount > 0) {
            LOGGER.info("🎨 @InjectImage em " + clazz.getSimpleName() +
                    ": " + injectedCount + " injetadas, " + skippedCount + " ignoradas, " + errorCount + " erros");
        }
    }

    /**
     * Injeta uma imagem em um ImageView baseado na anotação @InjectImage.
     */
    private static boolean injectImage(ImageView imageView, InjectImage ann) {
        String imageId = ann.value();

        if (imageId == null || imageId.isEmpty()) {
            LOGGER.warning("⚠️ @InjectImage com ID vazio");
            return false;
        }

        try {
            // Obtém o ImageManager via DiContainer
            ImageManager imageManager = DiContainer.getInstance().getBean(ImageManager.class);

            if (imageManager == null) {
                LOGGER.warning("⚠️ ImageManager não disponível");
                return false;
            }

            // Carrega a imagem
            Image image = imageManager.loadImage(imageId);

            if (image == null) {
                LOGGER.warning("⚠️ Imagem não encontrada: '" + imageId + "'");
                return false;
            }

            // Aplica redimensionamento se especificado na anotação
            double width = ann.width();
            double height = ann.height();

            if (width > 0 && height > 0) {
                imageView.setImage(new Image(image.getUrl(), width, height, ann.preserveRatio(), ann.smooth()));
            } else if (width > 0) {
                imageView.setFitWidth(width);
                imageView.setPreserveRatio(ann.preserveRatio());
                imageView.setImage(image);
            } else if (height > 0) {
                imageView.setFitHeight(height);
                imageView.setPreserveRatio(ann.preserveRatio());
                imageView.setImage(image);
            } else {
                imageView.setImage(image);
            }

            LOGGER.fine("✅ Imagem '" + imageId + "' injetada com sucesso");
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Erro ao injetar imagem '" + imageId + "': " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Injeta uma imagem por ID em um ImageView (sem anotação).
     */
    public static boolean injectImage(ImageView imageView, String imageId) {
        if (imageView == null || imageId == null || imageId.isEmpty()) return false;
        try {
            ImageManager imageManager = DiContainer.getInstance().getBean(ImageManager.class);
            if (imageManager != null) {
                Image image = imageManager.loadImage(imageId);
                if (image != null) {
                    imageView.setImage(image);
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Erro: " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * Injeta uma imagem com dimensões específicas.
     */
    public static boolean injectImage(ImageView imageView, String imageId, double width, double height) {
        if (imageView == null || imageId == null || imageId.isEmpty()) return false;
        try {
            ImageManager imageManager = DiContainer.getInstance().getBean(ImageManager.class);
            if (imageManager != null) {
                Image image = imageManager.loadImage(imageId);
                if (image != null) {
                    if (width > 0 && height > 0) {
                        imageView.setImage(new Image(image.getUrl(), width, height, true, true));
                    } else {
                        imageView.setImage(image);
                        if (width > 0) imageView.setFitWidth(width);
                        if (height > 0) imageView.setFitHeight(height);
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Erro: " + e.getMessage(), e);
        }
        return false;
    }
}