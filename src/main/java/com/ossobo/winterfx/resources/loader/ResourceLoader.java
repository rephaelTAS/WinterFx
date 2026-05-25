package com.ossobo.winterfx.resources.loader;

import com.ossobo.winterfx.resources.descriptor.ImageDescriptor;
import com.ossobo.winterfx.resources.descriptor.ViewDescriptor;
import com.ossobo.winterfx.resources.enums.CssMode;
import com.ossobo.winterfx.resources.excecoes.ResourceLoadException;
import com.ossobo.winterfx.resources.registry.ResourceRegistry;
import com.ossobo.winterfx.resources.resolver.ResourceResolver;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 🔧 ResourceLoader v2.0
 *
 * Conjunto de helpers para carregamento de recursos específicos.
 * Totalmente integrado com ViewDescriptor e ImageDescriptor.
 *
 * <pre>
 * Uso típico:
 *   // Carregar view com todas as configurações do descriptor
 *   FxmlLoadResult&lt;MyController&gt; result = loader.loadView("usuarios");
 *   Parent root = result.root();
 *   MyController controller = result.controller();
 *
 *   // Carregar imagem com dimensões do descriptor
 *   Image logo = loader.loadImage("logo");
 *
 *   // Carregar imagem como ImageView configurado
 *   ImageView iconView = loader.loadImageView("icon-save", 24, 24);
 *
 *   // Carregar imagem como Background
 *   Background bg = loader.loadBackground("bg-main");
 * </pre>
 */
public final class ResourceLoader {

    private static final Logger LOGGER = Logger.getLogger(ResourceLoader.class.getName());

    private final ResourceResolver resolver;
    private final ResourceRegistry registry;

    /**
     * Construtor completo com resolver e registry.
     */
    public ResourceLoader(ResourceResolver resolver) {
        this.resolver = resolver;
        this.registry = null;
        LOGGER.info("🔧 ResourceLoader v2.0 inicializado");
    }

    /**
     * Construtor com acesso ao registry para metadados.
     */
    public ResourceLoader(ResourceResolver resolver, ResourceRegistry registry) {
        this.resolver = resolver;
        this.registry = registry;
        LOGGER.info("🔧 ResourceLoader v2.0 inicializado (com registry)");
    }

    // =============================================
    // FXML / VIEWS
    // =============================================

    /**
     * Carrega um arquivo FXML como Parent (modo simples).
     *
     * @param viewId ID da view registrada
     * @return Parent (raiz da cena)
     * @throws ResourceLoadException Se falhar ao carregar
     */
    public Parent loadFxml(String viewId) {
        URL url = resolver.getViewUrl(viewId);

        try {
            FXMLLoader loader = new FXMLLoader(url);
            return loader.load();
        } catch (IOException e) {
            throw new ResourceLoadException(viewId, "FXML", e);
        }
    }

    /**
     * Carrega FXML com controller tipado.
     *
     * @param viewId         ID da view registrada
     * @param controllerType Tipo esperado do controller
     * @return Resultado contendo root e controller
     */
    public <T> FxmlLoadResult<T> loadFxmlWithController(String viewId, Class<T> controllerType) {
        URL url = resolver.getViewUrl(viewId);

        try {
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            Object controller = loader.getController();

            if (controllerType.isInstance(controller)) {
                return new FxmlLoadResult<>(root, controllerType.cast(controller));
            } else {
                throw new ResourceLoadException(
                        viewId, "FXML",
                        "Controller não é do tipo esperado: " + controllerType.getName()
                );
            }
        } catch (IOException e) {
            throw new ResourceLoadException(viewId, "FXML", e);
        }
    }

    /**
     * Carrega uma view completa usando ViewDescriptor.
     * Aplica CSS, style classes e outras configurações automaticamente.
     *
     * @param viewId ID da view registrada
     * @return FxmlLoadResult com root e controller
     */
    public FxmlLoadResult<?> loadView(String viewId) {
        // Obtém o descriptor
        Optional<ViewDescriptor> optDescriptor = resolver.resolveTyped(viewId, ViewDescriptor.class);

        if (optDescriptor.isEmpty()) {
            throw new ResourceLoadException(viewId, "VIEW", "ViewDescriptor não encontrado");
        }

        ViewDescriptor descriptor = optDescriptor.get();
        URL url = descriptor.getFxmlUrl();

        try {
            FXMLLoader loader = new FXMLLoader(url);

            // Resource bundle para i18n
            if (descriptor.getResourceBundle() != null && !descriptor.getResourceBundle().isEmpty()) {
                java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle(
                        descriptor.getResourceBundle()
                );
                loader.setResources(bundle);
            }

            Parent root = loader.load();
            Object controller = loader.getController();

            // Aplica CSS conforme configurado
            applyCss(root, descriptor);

            // Aplica style classes
            applyStyleClasses(root, descriptor);

            return new FxmlLoadResult<>(root, controller);

        } catch (IOException e) {
            throw new ResourceLoadException(viewId, "VIEW", e);
        }
    }

    /**
     * Carrega view e cria um Stage configurado.
     *
     * @param viewId ID da view registrada
     * @return Stage pronto para ser mostrado
     */
    public Stage loadViewInStage(String viewId) {
        FxmlLoadResult<?> result = loadView(viewId);

        Optional<ViewDescriptor> optDescriptor = resolver.resolveTyped(viewId, ViewDescriptor.class);
        ViewDescriptor descriptor = optDescriptor.orElseThrow();

        Stage stage = new Stage();
        stage.setTitle(descriptor.getTitle());

        Scene scene = new Scene(result.root(), descriptor.getWidth(), descriptor.getHeight());
        stage.setScene(scene);

        // Aplica estilo da janela
        if (descriptor.getStageStyle() != null) {
            stage.initStyle(descriptor.getStageStyle().toJavaFX());
        }

        // Configurações da janela
        stage.setResizable(descriptor.isResizable());
        stage.setAlwaysOnTop(descriptor.isAlwaysOnTop());
        stage.setMaximized(descriptor.isMaximized());

        if (descriptor.isCentered()) {
            stage.centerOnScreen();
        }

        return stage;
    }

    /**
     * Resultado do carregamento de FXML com controller.
     */
    public record FxmlLoadResult<T>(Parent root, T controller) {}

    // =============================================
    // IMAGENS
    // =============================================

    /**
     * Carrega uma imagem com configurações do ImageDescriptor.
     *
     * @param imageId ID da imagem registrada
     * @return Image carregada
     */
    public Image loadImage(String imageId) {
        Optional<ImageDescriptor> optDescriptor = Optional.empty();

        if (registry != null) {
            optDescriptor = registry.findImageById(imageId);
        }

        URL url = resolver.getImageUrl(imageId);

        if (optDescriptor.isPresent()) {
            ImageDescriptor descriptor = optDescriptor.get();
            return loadImageFromDescriptor(url, descriptor);
        }

        // Fallback: carrega com configurações padrão
        return new Image(url.toExternalForm());
    }

    /**
     * Carrega imagem com dimensões específicas (sobrescreve descriptor).
     */
    public Image loadImage(String imageId, double width, double height,
                           boolean preserveRatio, boolean smooth) {
        URL url = resolver.getImageUrl(imageId);
        return new Image(url.toExternalForm(), width, height, preserveRatio, smooth);
    }

    /**
     * Carrega imagem a partir do ImageDescriptor.
     */
    private Image loadImageFromDescriptor(URL url, ImageDescriptor descriptor) {
        double width = descriptor.getPreferredWidth();
        double height = descriptor.getPreferredHeight();
        boolean preserveRatio = descriptor.isPreserveRatio();
        boolean smooth = descriptor.isSmooth();

        if (width > 0 && height > 0) {
            return new Image(url.toExternalForm(), width, height, preserveRatio, smooth);
        } else if (width > 0) {
            return new Image(url.toExternalForm(), width, 0, preserveRatio, smooth);
        } else if (height > 0) {
            return new Image(url.toExternalForm(), 0, height, preserveRatio, smooth);
        } else {
            return new Image(url.toExternalForm());
        }
    }

    /**
     * Carrega imagem como ImageView configurado.
     *
     * @param imageId        ID da imagem registrada
     * @param overrideWidth  Largura desejada (-1 = usar descriptor)
     * @param overrideHeight Altura desejada (-1 = usar descriptor)
     * @return ImageView configurado
     */
    public ImageView loadImageView(String imageId, double overrideWidth, double overrideHeight) {
        Optional<ImageDescriptor> optDescriptor = Optional.empty();

        if (registry != null) {
            optDescriptor = registry.findImageById(imageId);
        }

        URL url = resolver.getImageUrl(imageId);
        Image image;
        ImageView imageView = new ImageView();

        if (optDescriptor.isPresent()) {
            ImageDescriptor descriptor = optDescriptor.get();

            double width = overrideWidth > 0 ? overrideWidth : descriptor.getPreferredWidth();
            double height = overrideHeight > 0 ? overrideHeight : descriptor.getPreferredHeight();
            boolean preserveRatio = descriptor.isPreserveRatio();
            boolean smooth = descriptor.isSmooth();

            if (width > 0 && height > 0) {
                image = new Image(url.toExternalForm(), width, height, preserveRatio, smooth);
            } else {
                image = new Image(url.toExternalForm());
            }

            imageView.setPreserveRatio(preserveRatio);
            imageView.setSmooth(smooth);

            if (width > 0) imageView.setFitWidth(width);
            if (height > 0) imageView.setFitHeight(height);

        } else {
            image = new Image(url.toExternalForm());
            if (overrideWidth > 0) imageView.setFitWidth(overrideWidth);
            if (overrideHeight > 0) imageView.setFitHeight(overrideHeight);
        }

        imageView.setImage(image);
        return imageView;
    }

    /**
     * Carrega imagem como Background para panes.
     *
     * @param imageId ID da imagem registrada
     * @return Background configurado
     */
    public Background loadBackground(String imageId) {
        Image image = loadImage(imageId);

        BackgroundImage bgImage = new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                BackgroundSize.DEFAULT
        );

        return new Background(bgImage);
    }

    /**
     * Carrega imagem em background com callback.
     */
    public void loadImageAsync(String imageId, ImageLoadCallback callback) {
        URL url = resolver.getImageUrl(imageId);
        Image image = new Image(url.toExternalForm(), true);

        image.progressProperty().addListener((obs, old, progress) -> {
            if (progress.doubleValue() >= 1.0 && !image.isError()) {
                Platform.runLater(() -> callback.onLoaded(image));
            }
        });

        image.errorProperty().addListener((obs, old, error) -> {
            if (error) {
                Platform.runLater(() -> callback.onError(
                        new ResourceLoadException(imageId, "IMAGE", "Falha ao carregar imagem")
                ));
            }
        });
    }

    /**
     * Carrega múltiplas imagens em paralelo.
     */
    public CompletableFuture<List<Image>> loadImagesAsync(String... imageIds) {
        List<CompletableFuture<Image>> futures = new ArrayList<>();

        for (String imageId : imageIds) {
            futures.add(CompletableFuture.supplyAsync(() -> loadImage(imageId)));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList()
                );
    }

    @FunctionalInterface
    public interface ImageLoadCallback {
        void onLoaded(Image image);

        default void onError(ResourceLoadException e) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar imagem", e);
        }
    }

    // =============================================
    // CSS
    // =============================================

    /**
     * Obtém URL do CSS como string externa.
     */
    public String getCssExternalForm(String cssId) {
        return resolver.getCssUrl(cssId).toExternalForm();
    }

    /**
     * Obtém múltiplos CSS como lista de strings.
     */
    public List<String> getCssExternalForms(String... cssIds) {
        List<String> result = new ArrayList<>();
        for (String cssId : cssIds) {
            result.add(getCssExternalForm(cssId));
        }
        return result;
    }

    /**
     * Aplica CSS de uma view ao root.
     */
    private void applyCss(Parent root, ViewDescriptor descriptor) {
        if (descriptor.getCssMode() == CssMode.NONE) return;

        if (descriptor.getCssMode() == CssMode.REPLACE) {
            root.getStylesheets().clear();
        }

        if (descriptor.getPrimaryCss() != null) {
            root.getStylesheets().add(descriptor.getPrimaryCss().toExternalForm());
        }

        if (descriptor.getAdditionalCss() != null) {
            for (URL cssUrl : descriptor.getAdditionalCss()) {
                root.getStylesheets().add(cssUrl.toExternalForm());
            }
        }
    }

    /**
     * Aplica style classes do descriptor.
     */
    private void applyStyleClasses(Parent root, ViewDescriptor descriptor) {
        if (descriptor.getStyleClasses() != null && !descriptor.getStyleClasses().isEmpty()) {
            root.getStyleClass().addAll(descriptor.getStyleClasses());
        }
    }

    // =============================================
    // SOM
    // =============================================

    /**
     * Carrega AudioClip para sons curtos.
     */
    public AudioClip loadAudioClip(String soundId) {
        URL url = resolver.getSoundUrl(soundId);
        return new AudioClip(url.toExternalForm());
    }

    /**
     * Carrega AudioClip com configurações.
     */
    public AudioClip loadAudioClip(String soundId, double volume, int priority) {
        AudioClip clip = loadAudioClip(soundId);
        clip.setVolume(volume);
        clip.setPriority(priority);
        return clip;
    }

    /**
     * Carrega MediaPlayer para sons longos.
     */
    public MediaPlayer loadMediaPlayer(String soundId) {
        URL url = resolver.getSoundUrl(soundId);
        Media media = new Media(url.toExternalForm());
        return new MediaPlayer(media);
    }

    // =============================================
    // UTILITÁRIOS
    // =============================================

    /**
     * Verifica se uma URL é acessível.
     */
    public boolean isAccessible(String resourceId) {
        try {
            resolver.resolveStream(resourceId).close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Pré-carrega uma view (útil para eager loading).
     */
    public void preloadView(String viewId) {
        LOGGER.info(() -> "⚡ Pré-carregando view: " + viewId);
        loadView(viewId);
    }

    /**
     * Pré-carrega uma imagem.
     */
    public void preloadImage(String imageId) {
        LOGGER.info(() -> "⚡ Pré-carregando imagem: " + imageId);
        loadImage(imageId);
    }

    /**
     * Obtém o ResourceResolver.
     */
    public ResourceResolver getResolver() {
        return resolver;
    }

    @Override
    public String toString() {
        return "ResourceLoader[v2.0, registry=" + (registry != null) + "]";
    }
}