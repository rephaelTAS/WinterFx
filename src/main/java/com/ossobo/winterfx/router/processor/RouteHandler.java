package com.ossobo.winterfx.router.processor;

import java.lang.reflect.Method;

/**
 * Registro imutável que armazena a associação entre uma instância de controlador
 * e um método alvo de roteamento interno.
 *
 * <p>Utiliza a sintaxe de {@code record} do Java para fornecer imutabilidade
 * e acessores automáticos com o mesmo nome dos campos (ex: {@code handler.method()}).</p>
 *
 * @param bean   A instância do controlador gerenciado pelo DI Container.
 * @param method O método anotado com {@code @GetMapping} ou {@code @PostMapping}.
 */
public record RouteHandler(Object bean, Method method) {}