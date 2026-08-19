package com.ossobo.winterfx.router.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Envelope padrão para o transporte de dados no sistema de roteamento interno do WinterFX.
 *
 * <p>Análogo ao {@code ResponseEntity} do Spring MVC, porém adaptado para aplicações JavaFX.
 * Todo método anotado com {@code @GetMapping} ou {@code @PostMapping} e invocado pelo
 * {@link com.ossobo.winterfx.router.processor.ApiDispatcher} deve retornar uma instância
 * desta classe.</p>
 *
 * <p>Estrutura padrão do envelope:</p>
 * <pre>
 * {
 *   "success": true/false,
 *   "message": "Mensagem opcional",
 *   "data": { "chave": valor, ... },
 *   "errors": { "campo": "Erro de validação", ... }
 * }
 * </pre>
 *
 * <p>Oferece uma API fluente (Builder Pattern) para facilitar a construção do objeto
 * de retorno diretamente no método do controlador.</p>
 */
public class ResponseData {

    private boolean success;
    private String message;
    private final Map<String, Object> data = new LinkedHashMap<>();
    private final Map<String, String> errors = new LinkedHashMap<>();

    /**
     * Construtor padrão que inicializa o estado da resposta como sucesso.
     */
    public ResponseData() {
        this.success = true;
    }

    /**
     * Método fábrica estático para criar uma resposta de sucesso rápida.
     *
     * @return Uma nova instância de {@code ResponseData} com {@code success = true}.
     */
    public static ResponseData success() {
        return new ResponseData();
    }

    /**
     * Método fábrica estático para criar uma resposta de erro rápida.
     *
     * @param message A mensagem descritiva do erro.
     * @return Uma nova instância de {@code ResponseData} com {@code success = false} e a mensagem definida.
     */
    public static ResponseData error(String message) {
        ResponseData response = new ResponseData();
        response.success = false;
        response.message = message;
        return response;
    }

    /**
     * Adiciona um par chave-valor ao mapa de dados da resposta.
     *
     * <p>Utilizado para retornar entidades, listas ou qualquer informação
     * relevante para a camada de visualização que invocou a rota.</p>
     *
     * @param key   A chave identificadora do dado (ex: "livros", "total").
     * @param value O valor a ser transportado (ex: uma List, um Integer, etc.).
     * @return A própria instância de {@code ResponseData}, permitindo encadeamento de chamadas.
     */
    public ResponseData withData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    /**
     * Adiciona um erro de validação ou de negócio específico a um campo.
     *
     * <p>Muito utilizado em rotas {@code @PostMapping} quando a validação
     * dos dados de um formulário falha.</p>
     *
     * @param field   O identificador do campo que gerou o erro (ex: "titulo", "isbn").
     * @param message A mensagem de erro amigável para exibição na UI.
     * @return A própria instância de {@code ResponseData}, permitindo encadeamento de chamadas.
     */
    public ResponseData withError(String field, String message) {
        this.errors.put(field, message);
        return this;
    }

    /**
     * Verifica se a operação representada por esta resposta foi executada com sucesso.
     *
     * @return {@code true} se sucesso, {@code false} caso contrário.
     */
    public boolean isSuccess() { return success; }

    /**
     * Retorna a mensagem global da resposta.
     *
     * @return A mensagem, ou nulo se não definida.
     */
    public String getMessage() { return message; }

    /**
     * Retorna o mapa imutável contendo todos os dados de retorno da operação.
     *
     * @return O mapa de dados.
     */
    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * Retorna o mapa imutável contendo os erros por campo, se houver.
     *
     * @return O mapa de erros de validação.
     */
    public Map<String, String> getErrors() {
        return Collections.unmodifiableMap(errors);
    }
}