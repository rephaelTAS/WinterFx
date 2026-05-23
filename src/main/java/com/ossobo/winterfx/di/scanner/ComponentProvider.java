package com.ossobo.winterfx.di.scanner;

@FunctionalInterface
public interface ComponentProvider<T> {
    T get();
}
