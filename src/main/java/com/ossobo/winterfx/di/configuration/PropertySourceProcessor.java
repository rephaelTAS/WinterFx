package com.ossobo.winterfx.di.configuration;

import com.ossobo.winterfx.anotations.PropertySource;
import com.ossobo.winterfx.scanner.registry.BeanRegistry;

import java.util.Comparator;
import java.util.Set;

/**
 * Carrega arquivos declarados via @PropertySource em classes escaneadas.
 *
 * <p>PRINCÍPIO: o framework fornece o MECANISMO; a aplicação fornece o
 * CONTEÚDO. Este processador não conhece nenhuma chave específica —
 * defaults de desenvolvimento pertencem ao placeholder: ${chave:default}.</p>
 *
 * <p>CONTRATO DE ORDEM: deve rodar APÓS o scan (BeanRegistry populado) e
 * ANTES da criação de qualquer bean com @Value.</p>
 *
 * @version 3.0
 */
public class PropertySourceProcessor {

    private static final System.Logger logger = System.getLogger(PropertySourceProcessor.class.getName());

    private final ConfigurationManager configurationManager;

    public PropertySourceProcessor(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    /**
     * Processa todas as classes do registry que declaram @PropertySource.
     */
    public void processPropertySources(BeanRegistry beanRegistry) {
        Set<Class<?>> classes = beanRegistry.getAllClasses();

        if (classes == null || classes.isEmpty()) {
            logger.log(System.Logger.Level.WARNING,
                    "⚠️ Nenhuma classe no BeanRegistry — @PropertySource não será processado");
            return;
        }

        // Ordenação por nome da classe: com múltiplos @PropertySource declarando
        // a mesma chave, a precedência passa a ser DETERMINÍSTICA (HashSet não
        // garante ordem de iteração entre execuções).
        classes.stream()
                .filter(c -> c.isAnnotationPresent(PropertySource.class))
                .sorted(Comparator.comparing(Class::getName))
                .forEach(this::carregar);

        logger.log(System.Logger.Level.INFO,
                "✅ @PropertySource processado. Total de chaves no ConfigurationManager: {0}",
                configurationManager.getPropertyCount());
    }

    /** Carrega o arquivo de uma classe, com erro sempre contextualizado. */
    private void carregar(Class<?> clazz) {
        PropertySource ps = clazz.getAnnotation(PropertySource.class);
        try {
            logger.log(System.Logger.Level.INFO,
                    "📄 Carregando: {0} (declarado em {1}, failIfNotFound={2})",
                    ps.value(), clazz.getSimpleName(), ps.failIfNotFound());
            configurationManager.loadPropertySource(ps.value(), ps.failIfNotFound());
        } catch (Exception e) {
            if (ps.failIfNotFound()) {
                // A exceção SEMPRE diz qual arquivo e qual classe declarou —
                // sem isso, apps com múltiplos @PropertySource não têm diagnóstico
                throw new IllegalStateException(
                        "Falha ao carregar @PropertySource '" + ps.value() +
                                "' declarado em " + clazz.getName(), e);
            }
            logger.log(System.Logger.Level.ERROR,
                    "❌ Falha ao carregar {0} (ignorada): {1}", ps.value(), e.getMessage());
        }
    }

    /**
     * Valida chaves obrigatórias (genérico — a APLICAÇÃO decide quais são).
     * O framework NUNCA deve chamar este método com chaves de domínio próprio.
     */
    public void validateRequiredProperties(String... requiredProperties) {
        StringBuilder missing = new StringBuilder();
        for (String property : requiredProperties) {
            if (!configurationManager.hasProperty(property)) {
                if (missing.length() > 0) missing.append(", ");
                missing.append(property);
            }
        }
        if (missing.length() > 0) {
            throw new IllegalStateException(
                    "Propriedades obrigatórias não configuradas: [" + missing +
                            "] — verifique o arquivo de propriedades");
        }
    }
}