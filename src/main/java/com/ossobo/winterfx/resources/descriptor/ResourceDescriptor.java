package com.ossobo.winterfx.resources.descriptor;

import com.ossobo.winterfx.resources.enums.ResourceOrigin;
import com.ossobo.winterfx.resources.enums.ResourceType;

import java.net.URL;

/**
 * Contrato base para descritores no Java 17+.
 * Usa nomes de acessadores de Record (sem o prefixo 'get').
 *
 * <p>Substitui a antiga classe abstrata ResourceDescriptor.
 * Records não podem estender classes, apenas implementar interfaces.</p>
 */
public interface ResourceDescriptor {
    String id();
    URL url();
    ResourceType resourceType();
    ResourceOrigin origin();
}