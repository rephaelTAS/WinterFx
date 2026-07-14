package com.ossobo.winterfx.di.resolver.methods;

import java.util.HashSet;
import java.util.Set;

/**
 * Detector de dependências circulares baseado na pilha de resolução da thread atual.
 *
 * <p>Este componente mantém um registro das classes que estão atualmente passando
 * pelo processo de resolução de dependências. Se o resolvedor tentar resolver uma
 * classe que já consta neste registro, significa que existe um ciclo de dependência
 * (ex: A depende de B, que depende de A).</p>
 *
 * <p>A utilização de {@link ThreadLocal} garante que o rastreamento seja estritamente
 * isolado por thread, prevenindo falsos positivos em ambientes concorrentes onde
 * múltiplas threads resolvem dependências simultaneamente.</p>
 */
public class CircularDependencyDetector {

    private final ThreadLocal<Set<Class<?>>> resolutionStack =
            ThreadLocal.withInitial(HashSet::new);

    /**
     * Verifica se um determinado tipo de classe está atualmente na pilha de resolução
     * da thread atual.
     *
     * <p>Retornar {@code true} neste método indica uma dependência circular em formação,
     * sinalizando ao resolvedor que ele deve interromper o processo e lançar a exceção
     * apropriada.</p>
     *
     * @param type O tipo (Classe) cujo status de resolução será verificado.
     * @return {@code true} se o tipo já estiver sendo resolvido nesta thread,
     *         {@code false} caso contrário.
     */
    public boolean isResolving(Class<?> type) {
        return resolutionStack.get().contains(type);
    }

    /**
     * Remove um tipo da pilha de resolução da thread atual.
     *
     * <p>Deve ser invocado dentro de um bloco {@code finally} após a conclusão
     * bem-sucedida da resolução e instanciação do bean, marcando-o como "resolvido".</p>
     *
     * @param type O tipo (Classe) que foi resolvido com sucesso.
     */
    public void endResolution(Class<?> type) {
        resolutionStack.get().remove(type);
    }

    /**
     * Adiciona um tipo à pilha de resolução da thread atual.
     *
     * <p>Deve ser invocado no início do processo de resolução de um bean, antes
     * de tentar resolver suas dependências construtoras ou de campos.</p>
     *
     * @param type O tipo (Classe) que começará a ser resolvido.
     */
    public void startResolution(Class<?> type) {
        resolutionStack.get().add(type);
    }
}