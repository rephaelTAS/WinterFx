package com.ossobo.winterfx.router.model;

import com.ossobo.winterfx.anotations.UI;
import javafx.css.Styleable;
import java.lang.reflect.Parameter;

/**
 * Resolvedor de parâmetros para o canal UI.
 *
 * Contrato do canal UI — ordem das guardas = ordem da causa:
 * 1) chave ausente/nula     → pareamento caller↔handler quebrado
 * 2) não-Styleable          → objeto de DADOS tentando cruzar o canal UI
 * 3) tipo incompatível      → componente certo, parâmetro declarado errado
 *
 * Desde 20.1.0: chave ausente/nula lança RouteBindingException
 * (antes retornava null, delegando a NPE ao handler).
 *
 * @see UI
 * @see RouteRequest
 */
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

        // Guarda 1: pareamento caller ↔ handler
        if (value == null) {
            throw new RouteBindingException(String.format(
                    "Chave @UI(\"%s\") ausente ou nula no RouteRequest. " +
                            "Todo parâmetro @UI exige o par correspondente enviado pelo caller.",
                    key));
        }

        // Guarda 2: domínio — apenas componentes de UI atravessam o canal UI
        if (!(value instanceof Styleable)) {
            throw new RouteBindingException(String.format(
                    "Parâmetro @UI(\"%s\") deve ser um componente JavaFX " +
                            "(Node, TableColumn, MenuItem, Tab, Tooltip, ContextMenu...), mas é %s",
                    key, value.getClass().getName()));
        }

        // Guarda 3: contrato de tipo — o handler declarou o que espera
        if (!p.getType().isInstance(value)) {
            throw new RouteBindingException(String.format(
                    "Componente @UI(\"%s\") incompatível: esperado %s, recebido %s",
                    key, p.getType().getSimpleName(), value.getClass().getSimpleName()));
        }

        return value;
    }
}