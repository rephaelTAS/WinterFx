package com.ossobo.winterfx.uiRefresh.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Params {

    private final Map<String, Object> values = new LinkedHashMap<>();

    private Params() {}

    public static Params empty() {
        return new Params();
    }

    public static Params with(String key, Object value) {
        return new Params().and(key, value);
    }

    public Params and(String key, Object value) {
        values.put(key, value);
        return this;
    }

    // Retorna uma visão imutável para o Dispatcher
    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(values);
    }

    // Mantém o build() caso prefira esse nome
    public Map<String, Object> build() {
        return toMap();
    }
}