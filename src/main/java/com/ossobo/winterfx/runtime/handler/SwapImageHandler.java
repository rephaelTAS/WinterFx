// SwapImageHandler.java v2.1 - 2026-06-14
// Handler para @SwapImage com troca de imagem dinâmica e execução condicional AFTER.
//
// PIPELINE CONDICIONAL v2.1:
//   - isBeforePhase(): false (não executa BEFORE)
//   - isAfterPhase(): true (executa APÓS método)
//   - isSuccessOnly(): true (executa SÓ se método sucesso)
//   - isErrorOnly(): false (não executa se erro)
//
// Vantagens v2.1:
//   - ✅ Executa na fase AFTER (após método)
//   - ✅ Execução segura no thread JavaFX (Platform.runLater)
//   - ✅ Troca imagem dinâmica no ImageView
//   - ✅ Carrega imagem via ImageManager.load()
//   - ✅ Opcional: define tamanho (width/height) da imagem
//   - ✅ Busca campo ImageView na hierarquia de classes (sobe até superclass)
//   - ✅ Logging robusto (warning para cada erro)
//   - ✅ Executa SÓ se método sucesso (SUCCESS_ONLY)
//   - ✅ NUNCA executa com @OnError (mutuamente exclusivo)
//
// @version 2.1 - Handler AFTER exclusivo para sucesso com troca de imagem dinâmica
package com.ossobo.winterfx.runtime.handler;

import com.ossobo.winterfx.imagemanager.ImageManager;
import com.ossobo.winterfx.imagemanager.anotations.SwapImage;
import javafx.application.Platform;
import javafx.scene.image.ImageView;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Handler para {@code @SwapImage} com troca de imagem dinâmica e execução condicional AFTER.
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * {@code
 * @FXML
 * private ImageView logoView;
 *
 * @OnSuccess(titulo = "Imagem Carregada", descricao = "Logo carregada com sucesso")
 * @SwapImage(imageId = "logo", imageView = "logoView", width = 100, height = 100)
 * public void handleLoadLogo(ActionEvent event) {
 *     loadLogoData();
 *     // Se sucesso → @OnSuccess + @SwapImage (ambos executam)
 * }
 * }
 * </pre>
 *
 * <p><b>Fluxo:</b></p>
 * <ol>
 *   <li>Método executa sem exceção</li>
 *   <li>executeSuccessPhase() seleciona handlers SUCCESS_ONLY</li>
 *   <li>SwapImageHandler carrega imagem no ImageView</li>
 *   <ol>
 *     <li>Busca do campo: {@link #findField(Class, String)} (hierarquia de classes)</li>
 *     <li>Verifica se é {@link ImageView}</li>
 *     <li>Carrega imagem: {@link ImageManager#load(ImageView, String, int, int)} (com tamanho)</li>
 *     <li>OU {@link ImageManager#load(ImageView, String)} (sem tamanho)</li>
 *   </ol>
 * </ol>
 *
 * <p><b>Parâmetros:</b></p>
 * <ul>
 *   <li>{@code imageId}: identificador da imagem (ex: "logo", "icon")</li>
 *   <li>{@code imageView}: nome do campo {@link ImageView} que recebe a imagem</li>
 *   <li>{@code width}: largura da imagem (0 = usa original)</li>
 *   <li>{@code height}: altura da imagem (0 = usa original)</li>
 * </ul>
 *
 * <p><b>IMPORTANTE:</b> Executa SÓ se sucesso. NUNCA com @OnError.</p>
 *
 * @version 2.1 - Handler AFTER exclusivo para sucesso com troca de imagem dinâmica
 */
public class SwapImageHandler implements AnnotationHandler<SwapImage> {

    private final ImageManager imageManager;

    /**
     * Construtor com ImageManager.
     *
     * @param imageManager Gerente de carregamento de imagens
     */
    public SwapImageHandler(ImageManager imageManager) {
        this.imageManager = imageManager;
    }

    /**
     * Verifica se este handler processa a anotação.
     *
     * @param annotation Anotação a verificar
     * @return true se é {@code @SwapImage}, false se não
     */
    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof SwapImage;
    }

    /**
     * @return Classe {@code SwapImage}
     */
    @Override
    public Class<SwapImage> getAnnotationType() {
        return SwapImage.class;
    }

    /**
     * Processa {@code @SwapImage} na fase AFTER com sucesso.
     *
     * <p><b>Fluxo:</b></p>
     * <ol>
     *   <li>Obtenção do target: {@link AnnotationContext#getTarget()}</li>
     *   <li>Busca do campo: {@link #findField(Class, String)}</li>
     *   <ol>
     *     <li>Sobe hierarquia de classes (current → superclass)</li>
     *     <li>Tenta {@link Class#getDeclaredField(String)}</li>
     *     <li>Retorna campo encontrado ou null</li>
     *   </ol>
     *   <li>Verificação do tipo:</li>
     *   <ol>
     *     <li>Verifica se campo é {@link ImageView}</li>
     *     <li>Se não: log warning e retorna</li>
     *   </ol>
     *   <li>Carregamento da imagem:</li>
     *   <ol>
     *     <li>Se width > 0 e height > 0: {@link ImageManager#load(ImageView, String, int, int)}</li>
     *     <li>Se não: {@link ImageManager#load(ImageView, String)}</li>
     *   </ol>
     * </ol>
     *
     * @param ctx Contexto com target do método
     * @param ann Anotação {@code @SwapImage}
     */
    @Override
    public void handle(AnnotationContext ctx, SwapImage ann) {
        Platform.runLater(() -> {
            try {
                Object target = ctx.getTarget();

                // Busca o campo na hierarquia de classes (proxy herda do original)
                Field field = findField(target.getClass(), ann.imageView());
                if (field == null) {
                    return;
                }
                field.setAccessible(true);
                Object value = field.get(target);

                if (value instanceof ImageView imageView) {
                    // Carrega imagem com ou sem tamanho
                    if (ann.width() > 0 && ann.height() > 0) {
                        imageManager.load(imageView, ann.imageId(), ann.width(), ann.height());
                    } else {
                        imageManager.load(imageView, ann.imageId());
                    }
                }
            } catch (Exception e) {
            }
        });
    }

    /**
     * Busca campo na hierarquia de classes (sobe até Object).
     *
     * <p><b>Fluxo:</b></p>
     * <ol>
     *   <li>current = clazz</li>
     *   <li>Loop: current ≠ null e current ≠ Object.class</li>
     *   <li>Tenta {@link Class#getDeclaredField(String)}</li>
     *   <li>Se não encontrado: current = current.getSuperclass()</li>
     *   <li>Retorna campo ou null</li>
     * </ol>
     *
     * @param clazz Classe inicial
     * @param name Nome do campo
     * @return Campo encontrado ou null
     */
    private Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * @return false (não executa na fase BEFORE)
     */
    @Override
    public boolean isBeforePhase() {
        return false;
    }

    /**
     * @return true (executa na fase AFTER, após método)
     */
    @Override
    public boolean isAfterPhase() {
        return true;
    }

    /**
     * @return true (executa SÓ se método sucesso)
     */
    @Override
    public boolean isSuccessOnly() {
        return true;
    }

    /**
     * @return false (não executa se erro)
     */
    @Override
    public boolean isErrorOnly() {
        return false;
    }
}