package com.ossobo.winterfx.di.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Gerenciador centralizado de configurações.
 * Versão 4.1 - Java 17+ com Fail-Fast, System.Logger e suporte a placeholders em textos mistos.
 */
public final class ConfigurationManager {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");
    private static final System.Logger LOGGER = System.getLogger(ConfigurationManager.class.getName());

    private final Properties properties = new Properties();
    private final String configFile;

    public ConfigurationManager(String configFile) {
        this.configFile = configFile;
    }

    public ConfigurationManager() {
        this(null);
    }

    public void loadConfiguration() {
        if (configFile != null && !configFile.isBlank()) {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFile)) {
                if (input != null) {
                    properties.load(input);
                } else {
                    LOGGER.log(System.Logger.Level.WARNING,
                            "Arquivo de configuração não encontrado no classpath: {0}", configFile);
                }
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Falha ao ler o arquivo de configuração: " + configFile, e);
            }
        }

        System.getenv().forEach((key, value) -> {
            String normalized = normalizeKey(key);
            if (!properties.containsKey(normalized)) {
                properties.put(normalized, value);
            }
        });

        System.getProperties().forEach((key, value) -> {
            String normalized = normalizeKey(key.toString());
            if (!properties.containsKey(normalized)) {
                properties.put(normalized, value);
            }
        });
    }

    private String normalizeKey(String key) {
        return key.toLowerCase().replace('_', '.');
    }

    /**
     * Resolve um placeholder individual.
     *
     * <p><b>Exemplos:</b></p>
     * <ul>
     *   <li>{@code resolvePlaceholder("${app.name}")} → "MeuApp"</li>
     *   <li>{@code resolvePlaceholder("${app.port:8080}")} → "8080" (ou valor do app.port)</li>
     *   <li>{@code resolvePlaceholder("A porta é ${app.port}")} → "A porta é 8080"</li>
     * </ul>
     *
     * @param expression A expressão contendo o placeholder
     * @return O valor resolvido ou a expressão original se não houver placeholder
     */
    public String resolvePlaceholder(String expression) {
        if (expression == null) return null;

        var matcher = PLACEHOLDER_PATTERN.matcher(expression);

        // ✅ CORREÇÃO: matcher.find() permite placeholders em textos mistos
        //    Ex: @Value("A porta é ${app.port}") → "A porta é 8080"
        if (!matcher.find()) {
            return expression;
        }

        var keyAndDefault = matcher.group(1);
        String key;
        String defaultValue = null;

        if (keyAndDefault.contains(":")) {
            var parts = keyAndDefault.split(":", 2);
            key = parts[0].trim();
            defaultValue = parts[1].trim();
        } else {
            key = keyAndDefault.trim();
        }

        var value = properties.getProperty(key);
        var resolved = value != null ? value : defaultValue;

        // ✅ Se o valor resolvido ainda contém placeholders, resolve recursivamente
        if (resolved != null && PLACEHOLDER_PATTERN.matcher(resolved).find()) {
            resolved = resolveRecursive(resolved);
        }

        // ✅ Substitui APENAS o placeholder na expressão original
        //    mantendo o texto ao redor
        return matcher.replaceFirst(resolved != null ? resolved : "");
    }

    /**
     * Resolve todos os placeholders em uma expressão de forma recursiva.
     *
     * <p>Exemplo:</p>
     * <ul>
     *   <li>{@code ${app.name}} → "MeuApp"</li>
     *   <li>{@code ${app.env}.${app.name}} → "prod.MeuApp"</li>
     *   <li>{@code ${app.url:${app.host}:${app.port}}} → "localhost:8080"</li>
     * </ul>
     *
     * @param expression A expressão contendo placeholders aninhados
     * @return A expressão com todos os placeholders resolvidos
     */
    public String resolveRecursive(String expression) {
        if (expression == null) return null;

        var result = expression;
        var matcher = PLACEHOLDER_PATTERN.matcher(result);
        var maxIterations = 10;
        var changed = true;

        // ✅ Resolve recursivamente até não haver mais placeholders
        while (changed && maxIterations-- > 0) {
            changed = false;
            matcher.reset(result);

            var sb = new StringBuilder();
            var lastEnd = 0;

            while (matcher.find()) {
                changed = true;
                var fullMatch = matcher.group(0);
                var keyAndDefault = matcher.group(1);

                String key;
                String defaultValue = null;

                if (keyAndDefault.contains(":")) {
                    var parts = keyAndDefault.split(":", 2);
                    key = parts[0].trim();
                    defaultValue = parts[1].trim();
                } else {
                    key = keyAndDefault.trim();
                }

                var value = properties.getProperty(key);
                var resolved = value != null ? value : defaultValue;

                // ✅ Se o valor resolvido ainda tem placeholder, resolve novamente
                if (resolved != null && PLACEHOLDER_PATTERN.matcher(resolved).find()) {
                    resolved = resolveRecursive(resolved);
                }

                sb.append(result, lastEnd, matcher.start());
                sb.append(resolved != null ? resolved : "");
                lastEnd = matcher.end();
            }

            if (changed) {
                sb.append(result, lastEnd, result.length());
                result = sb.toString();
            }
        }

        return result;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        var value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "Valor inválido para inteiro: {0}={1}", key, value);
            }
        }
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        var value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    public double getDouble(String key, double defaultValue) {
        var value = properties.getProperty(key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "Valor inválido para double: {0}={1}", key, value);
            }
        }
        return defaultValue;
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public int getPropertyCount() {
        return properties.size();
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public Map<String, String> getAllProperties() {
        Map<String, String> copy = new ConcurrentHashMap<>();
        properties.forEach((k, v) -> copy.put(k.toString(), v.toString()));
        return Map.copyOf(copy);
    }
}