package com.ossobo.winterfx.runtime.handler;

import java.lang.annotation.Annotation;

/**
 * Interface para handlers de anotações runtime.
 *
 * <p>Cada anotação ({@code @OnSuccess}, {@code @NewScene}, etc.)
 * tem seu próprio handler que implementa esta interface.</p>
 *
 * @param <A> Tipo da anotação que este handler processa
 */
public interface AnnotationHandler<A extends Annotation> {

    boolean supports(Annotation annotation);

    /** @return A classe da anotação que este handler processa */
    Class<A> getAnnotationType();

    /** Processa a anotação encontrada no método */
    void handle(AnnotationContext context, A annotation);
}