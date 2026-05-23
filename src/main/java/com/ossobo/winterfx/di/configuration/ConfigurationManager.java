package com.ossobo.winterfx.di.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ConfigurationManager v2.0
 *
 * Responsabilidade única: gerir configurações do container.
 *
 * - Carregar application.properties
 * - Resolver ${placeholders} com valores padrão
 * - Expor propriedades como String, int, boolean, List
 * - Fallback para variáveis de ambiente (System.getenv)
 * - Fallback para System.getProperty
 *
 * @since 2.0
 */
public final class ConfigurationManager {

    private static final Logger LOGGER = Logger.getLogger(ConfigurationManager.class.getName());

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    private final Properties properties = new Properties();
    private final String configFile;

    /**
     * Construtor com arquivo de configuração opcional.
     *
     * @param configFile nome do arquivo no classpath (ex: "application.properties")
     */
    public ConfigurationManager(String configFile) {
        this.configFile = configFile;
    }

    /**
     * Construtor vazio — sem arquivo de configuração.
     */
    public ConfigurationManager() {
        this(null);
    }

    // ===== CARREGAMENTO =====

    /**
     * Carrega as propriedades do arquivo e adiciona fallbacks.
     */
    public void loadConfiguration() {
        // 1. Arquivo de propriedades
        if (configFile != null && !configFile.isBlank()) {
            LOGGER.log(Level.INFO, "Carregando: {0}", configFile);
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFile)) {
                if (input != null) {
                    properties.load(input);
                    LOGGER.log(Level.INFO, "{0} propriedades carregadas.", properties.size());
                } else {
                    LOGGER.log(Level.WARNING, "Arquivo não encontrado: {0}", configFile);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Erro ao carregar " + configFile + ": " + e.getMessage());
            }
        }

        // 2. Variáveis de ambiente (fallback)
        System.getenv().forEach((key, value) -> {
            String normalized = normalizeKey(key);
            if (!properties.containsKey(normalized)) {
                properties.put(normalized, value);
            }
        });

        // 3. System properties (fallback final)
        System.getProperties().forEach((key, value) -> {
            String normalized = normalizeKey(key.toString());
            if (!properties.containsKey(normalized)) {
                properties.put(normalized, value);
            }
        });
    }

    /**
     * Normaliza chaves: APP_NAME → app.name
     */
    private String normalizeKey(String key) {
        return key.toLowerCase().replace('_', '.');
    }

    // ===== RESOLUÇÃO DE PLACEHOLDERS =====

    /**
     * Resolve um placeholder ou valor literal.
     *
     * Suporta:
     * - ${key} → busca nas propriedades
     * - ${key:default} → usa default se não encontrado
     * - valor literal → retorna o próprio valor
     *
     * @param expression expressão a resolver
     * @return valor resolvido, ou null
     */
    public String resolvePlaceholder(String expression) {
        if (expression == null) return null;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(expression);

        if (!matcher.matches()) {
            // Não é placeholder → retorna literal
            return expression;
        }

        String keyAndDefault = matcher.group(1);
        String key;
        String defaultValue = null;

        if (keyAndDefault.contains(":")) {
            String[] parts = keyAndDefault.split(":", 2);
            key = parts[0].trim();
            defaultValue = parts[1].trim();
        } else {
            key = keyAndDefault.trim();
        }

        String value = properties.getProperty(key);

        if (value != null) {
            return value;
        }

        if (defaultValue != null) {
            LOGGER.log(Level.FINE, "Usando default para '{0}': {1}", new Object[]{key, defaultValue});
            return defaultValue;
        }

        LOGGER.log(Level.WARNING, "Chave não encontrada: {0}", key);
        return null;
    }

    /**
     * Resolve um placeholder recursivamente (suporta ${a.${b}}).
     */
    public String resolveRecursive(String expression) {
        if (expression == null) return null;

        String result = expression;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(result);
        int maxIterations = 10;

        while (matcher.find() && maxIterations-- > 0) {
            String placeholder = matcher.group(0);
            String resolved = resolvePlaceholder(placeholder);
            if (resolved != null) {
                result = result.replace(placeholder, resolved);
                matcher = PLACEHOLDER_PATTERN.matcher(result);
            } else {
                break;
            }
        }

        return result;
    }

    // ===== ACESSO A PROPRIEDADES =====

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    public double getDouble(String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public int getPropertyCount() {
        return properties.size();
    }

    /**
     * Adiciona uma propriedade programaticamente.
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * Retorna todas as propriedades (cópia defensiva).
     */
    public Map<String, String> getAllProperties() {
        Map<String, String> copy = new ConcurrentHashMap<>();
        properties.forEach((k, v) -> copy.put(k.toString(), v.toString()));
        return copy;
    }
}