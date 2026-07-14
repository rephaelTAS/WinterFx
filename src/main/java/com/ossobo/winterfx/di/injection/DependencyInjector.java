package com.ossobo.winterfx.di.injection;

/**
 * Contrato base para injetores de dependência especializados do framework.
 *
 * <p>Cada implementação desta interface é responsável por interpretar e processar
 * um tipo específico de anotação de injeção (por exemplo, anotações de valor,
 * injeção de views, injeção de imagens, entre outras).</p>
 *
 * <p>O {@link InjectionManager} atua como o orquestrador central, iterando e
 * executando todos os injetores registrados na ordem em que foram adicionados
 * durante o processo de pós-criação dos beans.</p>
 *
 * @see InjectionManager
 */
public interface DependencyInjector {

    /**
     * Analisa a instância fornecida e realiza a injeção de dependências
     * conforme a lógica específica da implementação.
     *
     * @param instance A instância do bean que terá suas dependências injetadas.
     * @param type     O tipo (Classe) do bean sendo processado, útil para reflexão
     *                 cacheada ou validações de hierarquia.
     */
    void inject(Object instance, Class<?> type);
}