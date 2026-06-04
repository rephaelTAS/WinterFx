package com.ossobo.winterfx.runtime.handler;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registro central de handlers de anotações runtime.
 *
 * <p>Organiza os handlers em três grupos:</p>
 * <ul>
 *   <li><b>Before:</b> executados antes do método</li>
 *   <li><b>After:</b> executados depois do método (sucesso)</li>
 *   <li><b>Error:</b> executados depois do método (erro)</li>
 * </ul>
 */
public class HandlerRegistry {

    private final List<AnnotationHandler<?>> beforeHandlers = new CopyOnWriteArrayList<>();
    private final List<AnnotationHandler<?>> afterHandlers = new CopyOnWriteArrayList<>();
    private final List<AnnotationHandler<?>> errorHandlers = new CopyOnWriteArrayList<>();

    public void registerBefore(AnnotationHandler<?> handler) {
        beforeHandlers.add(handler);
    }

    public void registerAfter(AnnotationHandler<?> handler) {
        afterHandlers.add(handler);
    }

    public void registerError(AnnotationHandler<?> handler) {
        errorHandlers.add(handler);
    }

    /** Executa todos os handlers "before" cujas anotações estejam presentes no método. */
    @SuppressWarnings("unchecked")
    public void executeBefore(AnnotationContext context) {
        for (AnnotationHandler handler : beforeHandlers) {
            Annotation ann = context.getMethod().getAnnotation(handler.getAnnotationType());
            if (ann != null) {
                try {
                    handler.handle(context, ann);
                } catch (Exception ignored) {}
            }
        }
    }

    /** Executa todos os handlers "after" cujas anotações estejam presentes no método. */
    @SuppressWarnings("unchecked")
    public void executeAfter(AnnotationContext context) {
        for (AnnotationHandler handler : afterHandlers) {
            Annotation ann = context.getMethod().getAnnotation(handler.getAnnotationType());
            if (ann != null) {
                try {
                    handler.handle(context, ann);
                } catch (Exception ignored) {}
            }
        }
    }

    /** Executa todos os handlers "error" cujas anotações estejam presentes no método. */
    @SuppressWarnings("unchecked")
    public void executeError(AnnotationContext context) {
        for (AnnotationHandler handler : errorHandlers) {
            Annotation ann = context.getMethod().getAnnotation(handler.getAnnotationType());
            if (ann != null) {
                try {
                    handler.handle(context, ann);
                } catch (Exception ignored) {}
            }
        }
    }
}