package com.ossobo.winterfx.runtime;

import com.ossobo.winterfx.runtime.handler.AnnotationHandler;

import java.lang.annotation.Annotation;

// ============================================================
// PARTE 1: Singleton + Registro → WinterApplication (seu bootstrap)
// ============================================================
public class WinterApplication {
    private static volatile WinterApplication instance;
    private final HandlerRegistry handlerRegistry;
    private final WinterFXProxyFactory proxyFactory;

    private WinterApplication() {
        this.handlerRegistry = new HandlerRegistry();
        this.proxyFactory = new WinterFXProxyFactory(handlerRegistry);
    }

    public static WinterApplication getInstance() {
        WinterApplication local = instance;
        if (local == null) {
            synchronized (WinterApplication.class) {
                local = instance;
                if (local == null) {
                    local = new WinterApplication();
                    instance = local;
                }
            }
        }
        return local;
    }

    public HandlerRegistry getHandlerRegistry() { return handlerRegistry; }
    public WinterFXProxyFactory getProxyFactory() { return proxyFactory; }

    // Registro conveniente (delega ao registry)
    public <A extends Annotation> void registerHandler(AnnotationHandler<A> handler) {
        handlerRegistry.register(handler);
    }
}