package com.ossobo.winterfx.di.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gerenciador centralizado de configurações do WinterFX.
 *
 * <p>Ordem de precedência na resolução de uma chave:</p>
 * <ol>
 *   <li>System properties e variáveis de ambiente (carregadas no construtor,
 *       com normalização DATABASE_SQLITE_X → database.sqlite.x)</li>
 *   <li>Arquivos carregados via {@code @PropertySource} (primeiro vence)</li>
 *   <li>Default declarado no placeholder: {@code ${chave:valorPadrao}}</li>
 * </ol>
 *
 * <p>Esta classe é a FONTE de dados de configuração. Ela NÃO conhece
 * reflexão nem anotações — quem trabalha a injeção é o ValueInjector.</p>
 *
 * @version 5.1
 */
public final class ConfigurationManager {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");
    private static final System.Logger LOGGER = System.getLogger(ConfigurationManager.class.getName());

    /** Proteção contra referências circulares entre placeholders (a → b → a). */
    private static final int MAX_RECURSION = 10;

    private final Properties properties = new Properties();
    private final Properties defaultProperties = new Properties();
    private final Map<String, String> propertySources = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;

    public ConfigurationManager() {
        loadSystemProperties();
    }

    /**
     * Carrega um arquivo de propriedades do classpath.
     *
     * <p>REGRA DE PRECEDÊNCIA: "primeiro definido vence". Portanto este método
     * NUNCA deve ser chamado depois de defaults manuais — o arquivo de
     * propriedades deve ser a PRIMEIRA fonte externa carregada.</p>
     *
     * @param configFile     caminho do arquivo no classpath
     * @param failIfNotFound true = lança IllegalStateException se ausente
     */
    public void loadPropertySource(String configFile, boolean failIfNotFound) {
        if (configFile == null || configFile.isBlank()) {
            return;
        }

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (input == null) {
                if (failIfNotFound) {
                    throw new IllegalStateException(
                            "Arquivo de propriedades não encontrado no classpath: " + configFile);
                }
                LOGGER.log(System.Logger.Level.WARNING,
                        "⚠️ Arquivo de propriedades não encontrado (ignorado): {0}", configFile);
                return;
            }

            Properties props = new Properties();
            props.load(input);

            // Contador de diagnóstico: se o log mostrar "0 novas incorporadas",
            // alguma fonte anterior bloqueou todas as chaves — sintoma do bug
            // de precedência (defaults escritos antes do arquivo).
            AtomicInteger novas = new AtomicInteger();

            props.forEach((key, value) -> {
                String k = key.toString();
                if (!properties.containsKey(k)) {
                    properties.setProperty(k, value.toString());
                    propertySources.put(k, configFile);
                    novas.incrementAndGet();
                }
            });

            loaded = true;
            LOGGER.log(System.Logger.Level.INFO,
                    "📄 {0}: {1} propriedades no arquivo, {2} novas incorporadas",
                    configFile, props.size(), novas.get());

        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler o arquivo de configuração: " + configFile, e);
        }
    }

    /** Carrega system properties e env vars (fonte de MAIOR precedência). */
    private void loadSystemProperties() {
        System.getProperties().forEach((key, value) -> {
            String normalized = normalizeKey(key.toString());
            if (!properties.containsKey(normalized)) {
                properties.setProperty(normalized, value.toString());
                propertySources.put(normalized, "system");
            }
        });
        System.getenv().forEach((key, value) -> {
            String normalized = normalizeKey(key);
            if (!properties.containsKey(normalized)) {
                properties.setProperty(normalized, value);
                propertySources.put(normalized, "env");
            }
        });
    }

    /**
     * Locale.ROOT é obrigatório: em locales específicos (ex: turco),
     * "I".toLowerCase() produz "ı" e quebraria o matching de chaves.
     */
    private String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT).replace('_', '.');
    }

    /** Resolve {@code ${chave:default}} em uma expressão. Retorna "" se não resolver. */
    public String resolvePlaceholder(String expression) {
        if (expression == null) return null;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(expression);
        if (!matcher.find()) {
            return expression;
        }
        String replacement = resolveChave(matcher.group(1));

        // quoteReplacement: trata $ e \ do VALOR como texto literal.
        // Sem isso, um valor como "senha$123" quebraria o boot com
        // IllegalArgumentException (interpretado como grupo de regex).
        return matcher.replaceFirst(Matcher.quoteReplacement(replacement));
    }

    /**
     * Resolução recursiva de placeholders aninhados, uma camada por iteração.
     * O limite de iterações interrompe referências circulares.
     */
    public String resolveRecursive(String expression) {
        if (expression == null) return null;

        String result = expression;
        for (int i = 0; i < MAX_RECURSION; i++) {
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(result);
            if (!matcher.find()) {
                return result; // sem mais placeholders: resolvido
            }

            StringBuilder sb = new StringBuilder();
            matcher.reset();
            while (matcher.find()) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(resolveChave(matcher.group(1))));
            }
            matcher.appendTail(sb);

            if (sb.toString().equals(result)) {
                return result; // estável (ou circular) — evita loop infinito
            }
            result = sb.toString();
        }

        LOGGER.log(System.Logger.Level.WARNING,
                "Profundidade máxima de resolução atingida (possível referência circular): {0}", expression);
        return result;
    }

    /**
     * Resolve o conteúdo interno de um placeholder ("chave" ou "chave:default").
     * Método único de lookup — elimina duplicação entre resolvePlaceholder
     * e resolveRecursive (DRY).
     */
    private String resolveChave(String chaveComDefault) {
        String key = chaveComDefault;
        String defaultValue = null;

        int idx = chaveComDefault.indexOf(':');
        if (idx >= 0) {
            key = chaveComDefault.substring(0, idx).trim();
            defaultValue = chaveComDefault.substring(idx + 1).trim();
        }

        String value = properties.getProperty(key);
        if (value == null) {
            value = defaultProperties.getProperty(key);
        }

        String resolved = (value != null) ? value : defaultValue;
        if (resolved != null && PLACEHOLDER_PATTERN.matcher(resolved).find()) {
            resolved = resolveRecursive(resolved);
        }
        return resolved != null ? resolved : "";
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
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                LOGGER.log(System.Logger.Level.WARNING, "Valor inválido para inteiro: {0}={1}", key, value);
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
                LOGGER.log(System.Logger.Level.WARNING, "Valor inválido para double: {0}={1}", key, value);
            }
        }
        return defaultValue;
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key) || defaultProperties.containsKey(key);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        propertySources.put(key, "manual");
    }

    public Map<String, String> getAllProperties() {
        Map<String, String> copy = new ConcurrentHashMap<>();
        properties.forEach((k, v) -> copy.put(k.toString(), v.toString()));
        return Map.copyOf(copy);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public String getPropertySource(String key) {
        return propertySources.get(key);
    }

    /**
     * Total de chaves conhecidas. ATENÇÃO: inclui system properties e env vars
     * (~100+ chaves). Para métrica do arquivo, use o log do loadPropertySource.
     */
    public int getPropertyCount() {
        return properties.size();
    }

    /**
     * @deprecated Use o fluxo @PropertySource via PropertySourceProcessor.
     *             Mantido APENAS para o construtor legado do BootSequence.
     */
    @Deprecated
    public void loadConfiguration() {
        loadPropertySource("application.properties", false);
    }
}