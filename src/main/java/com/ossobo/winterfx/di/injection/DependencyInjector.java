package com.ossobo.winterfx.di.injection;

/**
 * Contrato para injetores de dependência.
 *
 * Cada implementação é responsável por um tipo específico
 * de injeção: @Value, @Inject, @InjectView, @InjectImage, etc.
 *
 * O InjectionManager orquestra a execução de todos os injectors
 * registrados, na ordem em que foram adicionados.
 */
public interface DependencyInjector {

    /**
     * Injeta dependências em uma instância.
     *
     * @param instance instância do bean a ser processada
     * @param type     classe do bean
     */
    void inject(Object instance, Class<?> type);
}