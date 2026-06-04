package com.ossobo.winterfx.scanner;

import com.ossobo.winterfx.scanner.registry.BeanRegistry;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

/**
 * Engine principal de escaneamento do WinterFX.
 *
 * <p>Executa UM único scan do ClassGraph e compartilha o ScanResult
 * com todos os scanners especializados.</p>
 */
public final class ScannerEngine {

    private final String[] basePackages;

    public ScannerEngine(String... packages) {
        this.basePackages = (packages != null && packages.length > 0)
                ? packages : new String[]{""};
    }

    public int scanAndRegister(BeanRegistry beanRegistry, ResourceRegistry resourceRegistry) {
        try (ScanResult scanResult = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .acceptPackages(basePackages)
                .scan()) {

            int beans = new BeanAnnotationScanner(scanResult).scanAndRegister(beanRegistry);
            int resources = new ResourceAnnotationScanner(scanResult).scanAndRegister(resourceRegistry);

            return beans + resources;
        }
    }
}