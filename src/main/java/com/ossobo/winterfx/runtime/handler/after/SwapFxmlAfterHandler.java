package com.ossobo.winterfx.runtime.handler.after;

import com.ossobo.winterfx.view.anotations.SwapFxml;
import com.ossobo.winterfx.runtime.handler.AnnotationContext;
import com.ossobo.winterfx.runtime.handler.AnnotationHandler;
import com.ossobo.winterfx.runtime.handler.before.SwapFxmlBeforeHandler;

/** Handler para @SwapFxml(before=false) — executado DEPOIS do método (sucesso). */
public class SwapFxmlAfterHandler implements AnnotationHandler<SwapFxml> {

    @Override
    public Class<SwapFxml> getAnnotationType() {
        return SwapFxml.class;
    }

    @Override
    public void handle(AnnotationContext context, SwapFxml annotation) {
        if (annotation.before()) return;
        SwapFxmlBeforeHandler.swap(context, annotation);
    }
}