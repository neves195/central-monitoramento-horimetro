package br.com.manutencao.central_manutencao.common;

/**
 * Regra de negócio violada (ex: código duplicado, horímetro menor que o atual).
 * O {@link GlobalExceptionHandler} converte em HTTP 409.
 */
public class BusinessRuleException extends RuntimeException {

    /** @param message texto devolvido ao cliente na resposta de erro */
    public BusinessRuleException(String message) {
        super(message);
    }
}
