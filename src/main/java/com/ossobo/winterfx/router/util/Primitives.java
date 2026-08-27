package com.ossobo.winterfx.router.util;

/**
 * Utilitário compartilhado para conversão de tipos primitivos
 * em seus wrappers correspondentes.
 *
 * <p>Extraído de {@code RouteVarResolver} para eliminar duplicação
 * (DRY), sendo agora consumido também pelo {@code PayloadResolver}.</p>
 */
public final class Primitives {

    private Primitives() {}

    /**
     * Retorna o wrapper da primitiva informada, ou a própria classe caso não seja primitiva.
     */
    public static Class<?> boxed(Class<?> type) {
        return switch (type.getName()) {
            case "int"     -> Integer.class;
            case "long"    -> Long.class;
            case "boolean" -> Boolean.class;
            case "double"  -> Double.class;
            case "float"   -> Float.class;
            case "short"   -> Short.class;
            case "byte"    -> Byte.class;
            case "char"    -> Character.class;
            default        -> type;
        };
    }
}