package com.ossobo.winterfx.view;

import com.ossobo.winterfx.view.loader.LoadedView;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class LazyViewProxy<T> implements InvocationHandler {
    private final StageManager stageManager;
    private final String viewId;
    private T target;
    private boolean loaded = false;

    public LazyViewProxy(StageManager stageManager, String viewId) {
        this.stageManager = stageManager;
        this.viewId = viewId;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 🔥 CARREGA SOB DEMANDA (APENAS QUANDO FOR USADO)
        if (!loaded) {
            LoadedView<?> loadedView = stageManager.loadView(viewId);
            target = (T) loadedView.getController();
            loaded = true;
        }
        return method.invoke(target, args);
    }
}