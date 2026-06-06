package com.ossobo.winterfx.runtime.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

    // 🆕 Verifica se o método tem anotações suportadas por handlers "before"
    public boolean supportsBefore(Method method) {
        for (AnnotationHandler<?> handler : beforeHandlers) {
            if (method.isAnnotationPresent(handler.getAnnotationType())) {
                return true;
            }
        }
        return false;
    }

    // 🆕 Verifica se o método tem anotações suportadas por handlers "after"
    public boolean supportsAfter(Method method) {
        for (AnnotationHandler<?> handler : afterHandlers) {
            if (method.isAnnotationPresent(handler.getAnnotationType())) {
                return true;
            }
        }
        return false;
    }

    // 🆕 Verifica se o método tem anotações suportadas por handlers "error"
    public boolean supportsError(Method method) {
        for (AnnotationHandler<?> handler : errorHandlers) {
            if (method.isAnnotationPresent(handler.getAnnotationType())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public void executeBefore(AnnotationContext context) {
        for (AnnotationHandler handler : beforeHandlers) {
            Annotation ann = context.getMethod().getAnnotation(handler.getAnnotationType());
            if (ann != null) {
                try { handler.handle(context, ann); } catch (Exception ignored) {}
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void executeAfter(AnnotationContext context) {
        for (AnnotationHandler handler : afterHandlers) {
            Annotation ann = context.getMethod().getAnnotation(handler.getAnnotationType());
            if (ann != null) {
                try { handler.handle(context, ann); } catch (Exception ignored) {}
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void executeError(AnnotationContext context) {
        for (AnnotationHandler handler : errorHandlers) {
            Annotation ann = context.getMethod().getAnnotation(handler.getAnnotationType());
            if (ann != null) {
                try { handler.handle(context, ann); } catch (Exception ignored) {}
            }
        }
    }
}