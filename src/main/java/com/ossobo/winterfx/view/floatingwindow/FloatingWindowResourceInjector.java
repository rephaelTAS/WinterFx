package com.ossobo.winterfx.view.floatingwindow;

import com.ossobo.winterfx.di.injection.DependencyInjector;
import com.ossobo.winterfx.view.floatingwindow.FloatingWindowManager;

/**
 * FloatingWindowResourceInjector v1.0
 *
 * <p>Injetor de janelas flutuantes via {@code @FloatingWindow}.
 * Implementa {@link DependencyInjector} para registro externo.</p>
 *
 * @version 1.0 (01/07/2026)
 */
public class FloatingWindowResourceInjector implements DependencyInjector {

    private final FloatingWindowManager floatingWindowManager;

    public FloatingWindowResourceInjector(FloatingWindowManager floatingWindowManager) {
        this.floatingWindowManager = floatingWindowManager;
    }

    @Override
    public void inject(Object instance, Class<?> type) {
        floatingWindowManager.processAnnotations(instance);
    }
}