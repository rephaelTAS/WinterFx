package com.ossobo.winterfx.scanner;

import com.ossobo.winterfx.scanner.registry.BeanRegistry;
import com.ossobo.winterfx.scanner.registry.ResourceRegistry;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class ScannerEngine {

    private static final Logger LOGGER = Logger.getLogger(ScannerEngine.class.getName());

    private final BeanAnnotationScanner beanScanner;
    private final ResourceAnnotationScanner resourceScanner;

    public ScannerEngine(String... packages) {
        this.beanScanner = new BeanAnnotationScanner(packages);
        this.resourceScanner = new ResourceAnnotationScanner(packages);
    }

    /**
     * Executa scan completo: beans + recursos.
     */
    public int scanAndRegister(BeanRegistry beanRegistry, ResourceRegistry resourceRegistry) {
        LOGGER.info("🔍 ScannerEngine — iniciando scan completo...");
        long startTime = System.currentTimeMillis();

        int beans = beanScanner.scanAndRegister(beanRegistry);
        int resources = resourceScanner.scanAndRegister(resourceRegistry);

        long duration = System.currentTimeMillis() - startTime;
        LOGGER.log(Level.INFO, "✅ Scan completo em {0}ms — Beans: {1}, Recursos: {2}",
                new Object[]{duration, beans, resources});

        return beans + resources;
    }
}