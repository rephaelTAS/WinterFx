package com.ossobo.winterfx.runtime.handler.after;

import com.ossobo.winterfx.imagemanager.anotations.SwapImage;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;
import com.ossobo.winterfx.runtime.handler.before.SwapImageBeforeHandler;

/**
 * Handler para @SwapImage(before=false) — executado DEPOIS do método.
 */
public class SwapImageAfterHandler implements AnnotationHandler<SwapImage> {

    @Override
    public Class<SwapImage> getAnnotationType() {
        return SwapImage.class;
    }

    @Override
    public void handle(AnnotationContext context, SwapImage annotation) {
        if (annotation.before()) return;
        SwapImageBeforeHandler.swap(context, annotation);
    }
}