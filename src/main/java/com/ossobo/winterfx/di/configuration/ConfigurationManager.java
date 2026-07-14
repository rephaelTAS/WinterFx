package com.ossobo.winterfx.di.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gerenciador centralizado de configurações do contêiner de injeção de dependências.
 *
 * <p>Esta classe é responsável por carregar, armazenar e resolver propriedades de
 * configuração a partir de múltiplas fontes, seguindo uma ordem de precedência estrita:</p>
 * <ol>
 *     <li>Arquivo de propriedades no classpath (ex: {@code application.properties})</li>
 *     <li>Variáveis de ambiente do sistema operacional</li>
 *     <li>Propriedades do sistema ({@code System.getProperties()})</li>
 * </ol>
 *
 * <p>Oferece suporte à resolução de placeholders no formato {@code ${chave}} e
 * {@code ${chave:valorPadrao}}, incluindo resolução recursiva para placeholders aninhados.</p>
 *
 * @since 2.0
 */
public final class ConfigurationManager {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    private final Properties properties = new Properties();
    private final String configFile;

    /**
     * Construtor que define o arquivo de configuração a ser carregado.
     *
     * @param configFile O nome do arquivo localizado no classpath (ex: {@code "application.properties"}).
     *                   Pode ser nulo para inicializar sem arquivo de propriedades.
     */
    public ConfigurationManager(String configFile) {
        this.configFile = configFile;
    }

    /**
     * Construtor padrão que inicializa o gerenciador sem nenhum arquivo de propriedades.
     * A configuração será composta apenas por variáveis de ambiente e propriedades do sistema.
     */
    public ConfigurationManager() {
        this(null);
    }

    // ===== CARREGAMENTO =====

    /**
     * Executa o carregamento das propriedades seguindo a cadeia de precedência.
     *
     * <p>O método não lança exceções em caso de ausência ou erro de leitura do arquivo,
     * falhando silenciosamente e prosseguindo para as fontes de fallback.</p>
     */
    public void loadConfiguration() {
        if (configFile != null && !configFile.isBlank()) {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFile)) {
                if (input != null) {
                    properties.load(input);
                }
            } catch (IOException e) {
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

    /**
     * Normaliza o formato de uma chave substituindo underscores por pontos
     * e convertendo para minúsculas (ex: {@code APP_NAME} torna-se {@code app.name}).
     *
     * @param key A chave original a ser normalizada.
     * @return A chave normalizada.
     */
    private String normalizeKey(String key) {
        return key.toLowerCase().replace('_', '.');
    }

    // ===== RESOLUÇÃO DE PLACEHOLDERS =====

    /**
     * Resolve uma expressão de placeholder ou retorna o valor literal.
     *
     * <p>Formatos suportados:</p>
     * <ul>
     *     <li>{@code ${chave}} - Retorna o valor associado à chave ou {@code null}.</li>
     *     <li>{@code ${chave:valorPadrao}} - Retorna o valor associado à chave,
     *         ou o {@code valorPadrao} caso a chave não exista.</li>
     *     <li>{@code valorLiteral} - Retorna a própria string se não corresponder ao padrão.</li>
     * </ul>
     *
     * @param expression A expressão a ser avaliada.
     * @return O valor resolvido, o valor padrão, o valor literal ou {@code null}.
     */
    public String resolvePlaceholder(String expression) {
        if (expression == null) return null;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(expression);

        if (!matcher.matches()) {
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
            return defaultValue;
        }

        return null;
    }

    /**
     * Resolve uma expressão de forma recursiva, permitindo a interpolação de
     * placeholders aninhados (ex: {@code ${a.${b}}}).
     *
     * <p>Possui um limite máximo de 10 iterações para prevenir loops infinitos
     * causados por referências circulares nas propriedades.</p>
     *
     * @param expression A expressão contendo placeholders a serem resolvidos.
     * @return A expressão completamente resolvida, ou o último estado válido em caso de falha.
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

    /**
     * Recupera o valor de uma propriedade específica.
     *
     * @param key A chave da propriedade.
     * @return O valor da propriedade como {@code String}, ou {@code null} se não existir.
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Recupera o valor de uma propriedade, retornando um valor padrão caso a chave não exista.
     *
     * @param key          A chave da propriedade.
     * @param defaultValue O valor a ser retornado se a propriedade não for encontrada.
     * @return O valor da propriedade ou o {@code defaultValue}.
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Recupera uma propriedade convertida para inteiro.
     *
     * @param key          A chave da propriedade.
     * @param defaultValue O valor retornado caso a propriedade não exista ou não seja um número válido.
     * @return O valor inteiro da propriedade ou o {@code defaultValue}.
     */
    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * Recupera uma propriedade convertida para booleano.
     *
     * @param key          A chave da propriedade.
     * @param defaultValue O valor retornado caso a propriedade não exista.
     * @return O valor booleano da propriedade ou o {@code defaultValue}.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    /**
     * Recupera uma propriedade convertida para ponto flutuante (double).
     *
     * @param key          A chave da propriedade.
     * @param defaultValue O valor retornado caso a propriedade não exista ou não seja um número válido.
     * @return O valor double da propriedade ou o {@code defaultValue}.
     */
    public double getDouble(String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * Verifica se uma determinada chave existe no contexto de configuração.
     *
     * @param key A chave a ser verificada.
     * @return {@code true} se a chave existir, {@code false} caso contrário.
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /**
     * Retorna a quantidade total de propriedades carregadas no gerenciador.
     *
     * @return O número de propriedades disponíveis.
     */
    public int getPropertyCount() {
        return properties.size();
    }

    /**
     * Adiciona ou sobrescreve uma propriedade de forma programática em tempo de execução.
     *
     * @param key   A chave da propriedade.
     * @param value O valor da propriedade.
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * Retorna uma cópia defensiva de todas as propriedades atualmente armazenadas.
     *
     * <p>Alterações no mapa retornado não afetarão o estado interno do gerenciador.</p>
     *
     * @return Um mapa imutável contendo todas as chaves e valores de configuração.
     */
    public Map<String, String> getAllProperties() {
        Map<String, String> copy = new ConcurrentHashMap<>();
        properties.forEach((k, v) -> copy.put(k.toString(), v.toString()));
        return copy;
    }
}