package com.ossobo.winterfx.di.resolver.methods;

import java.util.HashSet;
import java.util.Set;

/**
 * Componente que rastreia a pilha de resolução de dependências por thread
 * para detectar e prevenir dependências circulares.
 */
public class CircularDependencyDetector {

    // Usa ThreadLocal para garantir que o stack de resolução seja isolado por thread,
    // essencial para a robustez em ambientes multi-threaded.
    private final ThreadLocal<Set<Class<?>>> resolutionStack =
            ThreadLocal.withInitial(HashSet::new);

    /**
     * Verifica se um determinado tipo (Class) está atualmente na pilha de resolução
     * da Thread atual, indicando uma potencial dependência circular.
     * * @param type O tipo (Class) a ser verificado.
     * @return true se o tipo já está sendo resolvido nesta thread; false caso contrário.
     */
    public boolean isResolving(Class<?> type) {
        // RESOLVE O ERRO NA LINHA 226 do DependencyResolver
        return resolutionStack.get().contains(type);
    }



    /**
     * Remove um tipo da pilha de resolução, marcando-o como 'resolvido'.
     */
    public void endResolution(Class<?> type) {
        resolutionStack.get().remove(type);
    }

    /**
     * Adiciona um tipo à pilha de resolução, marcando-o como 'em resolução'.
     */
    public void startResolution(Class<?> type) {
        // Agora o método se chama startResolution(), resolvendo o erro de símbolo.
        resolutionStack.get().add(type);
    }
}