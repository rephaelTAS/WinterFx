package com.ossobo.winterfx.di.injection;

import com.ossobo.winterfx.view.floatingwindow.FloatingWindowManager;

public class FloatingWindowInjector implements DependencyInjector {

    private final FloatingWindowManager floatingWindowManager;

    public FloatingWindowInjector(FloatingWindowManager floatingWindowManager) {
        this.floatingWindowManager = floatingWindowManager;
    }

    @Override
    public void inject(Object instance, Class<?> type) {
        floatingWindowManager.processAnnotations(instance);
    }
}