package com.ossobo.winterfx.uiRefresh.processor;// WebRequestMappingProcessor.java


import com.ossobo.winterfx.anotations.GetMapping;
import com.ossobo.winterfx.anotations.PostMapping;
import com.ossobo.winterfx.uiRefresh.model.ResponseData;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebRequestMappingProcessor {

    private static final WebRequestMappingProcessor INSTANCE = new WebRequestMappingProcessor();

    private final Map<Class<?>, Map<String, Method>> getMappings = new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String, Method>> postMappings = new ConcurrentHashMap<>();

    private WebRequestMappingProcessor() {}

    public static WebRequestMappingProcessor getInstance() {
        return INSTANCE;
    }

    public void registerMappings(Object controller) {
        Class<?> clazz = controller.getClass();
        Map<String, Method> gets = new ConcurrentHashMap<>();
        Map<String, Method> posts = new ConcurrentHashMap<>();

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GetMapping.class)) {
                GetMapping annotation = method.getAnnotation(GetMapping.class);
                String path = annotation.value().isEmpty() ? method.getName() : annotation.value();
                gets.put(path, method);
            }

            if (method.isAnnotationPresent(PostMapping.class)) {
                PostMapping annotation = method.getAnnotation(PostMapping.class);
                String path = annotation.value().isEmpty() ? method.getName() : annotation.value();
                posts.put(path, method);
            }
        }

        if (!gets.isEmpty()) {
            getMappings.put(clazz, gets);
        }
        if (!posts.isEmpty()) {
            postMappings.put(clazz, posts);
        }
    }

    public ResponseData executeGet(Object controller, String path, Object... args) {
        Map<String, Method> mappings = getMappings.get(controller.getClass());
        if (mappings == null || !mappings.containsKey(path)) {
            return ResponseData.error("Método GET não encontrado: " + path);
        }

        Method method = mappings.get(path);
        try {
            method.setAccessible(true);
            Object result = method.invoke(controller, args);

            if (result instanceof ResponseData) {
                return (ResponseData) result;
            }
            return ResponseData.success().withData("result", result);

        } catch (Exception e) {
            return ResponseData.error("Erro ao executar GET: " + e.getMessage());
        }
    }

    public ResponseData executePost(Object controller, String path, Object... args) {
        Map<String, Method> mappings = postMappings.get(controller.getClass());
        if (mappings == null || !mappings.containsKey(path)) {
            return ResponseData.error("Método POST não encontrado: " + path);
        }

        Method method = mappings.get(path);
        try {
            method.setAccessible(true);
            Object result = method.invoke(controller, args);

            if (result instanceof ResponseData) {
                return (ResponseData) result;
            }
            return ResponseData.success().withData("result", result);

        } catch (Exception e) {
            return ResponseData.error("Erro ao executar POST: " + e.getMessage());
        }
    }

    public boolean hasGetMapping(Object controller, String path) {
        Map<String, Method> mappings = getMappings.get(controller.getClass());
        return mappings != null && mappings.containsKey(path);
    }

    public boolean hasPostMapping(Object controller, String path) {
        Map<String, Method> mappings = postMappings.get(controller.getClass());
        return mappings != null && mappings.containsKey(path);
    }
}