package com.ossobo.winterfx.imagemanager.image;

import com.ossobo.winterfx.di.annotations.Component;
import com.ossobo.winterfx.di.annotations.ScopeAnnotation;
import com.ossobo.winterfx.di.scopes.enums.ScopeType;


/**
 * 🎯 IMAGE UTILS - Classe utilitária para validações comuns
 * 🔥 ELIMINA DUPLICAÇÃO DE CÓDIGO
 */
public final class ImageUtils {

    /**
     * ✅ VALIDA CHAVE - Única implementação para todo o sistema
     */
    public static void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Chave não pode ser nula ou vazia");
        }
    }

    /**
     * ✅ VALIDA RESOURCE PATH
     */
    public static void validateResourcePath(String resourcePath) {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource path não pode ser nulo ou vazio");
        }
    }

    /**
     * ✅ EXTRACT FILENAME PARA LOGGING
     */
    public static String extractFilename(String path) {
        if (path == null) return "null";
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    // 🔥 CLASSE UTILITÁRIA - NÃO INSTANCIÁVEL
    private ImageUtils() {
        throw new AssertionError("Classe utilitária - não instanciar");
    }
}