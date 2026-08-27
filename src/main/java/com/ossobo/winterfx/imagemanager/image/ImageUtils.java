// ImageUtils.java v3.0 - 2026-08-22
// Classe utilitária com validações
package com.ossobo.winterfx.imagemanager.image;

import java.nio.file.Path;

/**
 * Classe utilitária para validações e operações comuns de imagem.
 *
 * @version 3.0 (22/08/2026) - Mantido com pequenos ajustes
 */
public final class ImageUtils {

    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult success() {
            return new ValidationResult(true, "Válido");
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }

    private ImageUtils() {
        throw new AssertionError("Classe utilitária - não instanciar");
    }

    public static void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Chave não pode ser nula ou vazia");
        }
    }

    public static void validateResourcePath(String resourcePath) {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource path não pode ser nulo ou vazio");
        }
    }

    public static String extractFilename(String path) {
        if (path == null) return "null";
        var lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    public static String normalizePath(String path) {
        if (path == null) return null;

        var normalized = path.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        // Garante que começa com '/'
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        // Remove '..' e '.' (simplificação básica)
        try {
            var pathObj = Path.of(normalized).normalize().toString();
            return pathObj.replace('\\', '/');
        } catch (Exception e) {
            return normalized;
        }
    }
}