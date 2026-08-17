package com.ossobo.winterfx.uiRefresh.model;

import com.ossobo.winterfx.anotations.UI;
import javafx.scene.Node;
import java.lang.reflect.Parameter;

public final class UIResolver implements ParameterResolver {

    @Override
    public boolean supports(Parameter p) {
        return p.isAnnotationPresent(UI.class);
    }

    @Override
    public Object resolve(Parameter p, RouteRequest request) {
        UI ann = p.getAnnotation(UI.class);
        String key = ann.value();
        Object value = request.get(key);

        if (value == null) {
            return null;
        }
        if (!(value instanceof Node node)) {
            throw new RouteBindingException(String.format(
                    "Parâmetro @UI(\"%s\") deve ser um Node JavaFX, mas é %s",
                    key, value.getClass().getName()));
        }
        if (!p.getType().isInstance(node)) {
            throw new RouteBindingException(String.format(
                    "Node @UI(\"%s\") incompatível: esperado %s, recebido %s",
                    key, p.getType().getSimpleName(), node.getClass().getSimpleName()));
        }
        return node;
    }
}